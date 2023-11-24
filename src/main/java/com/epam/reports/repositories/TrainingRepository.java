package com.epam.reports.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.epam.reports.entities.TrainingRequest;
import com.epam.reports.entities.TrainingSummary;

@Repository
public class TrainingRepository {
	
	@Autowired
	private DynamoDBMapper dynamoDBMapper;
	
	
	public void saveUpdateTraining(TrainingRequest trainingRequest) {
		if(existTraining(trainingRequest.getTrainerUsername()).isEmpty()) {
			saveTraining(trainingRequest);
		}else {
			System.out.println("Updating a trainer");
			updateTraining(trainingRequest);
		}
	}
	
	public void saveTraining(TrainingRequest trainingRequest) {
		TrainingSummary trainingSummary=new TrainingSummary();
		trainingSummary.setTrainerUsername(trainingRequest.getTrainerUsername());
		trainingSummary.setTrainerFirstName(trainingRequest.getTrainerFirstName());
		trainingSummary.setTrainerLastName(trainingRequest.getTrainerLastName());
		trainingSummary.setStatus(trainingRequest.getIsActive()==true?"Active":"Inactive");

		String trainingName=trainingRequest.getTrainingTypeEnum();
		Float durationTraining=trainingRequest.getTrainingDuration();
		TrainingSummary.TrainingSummaryDuration trainingSummaryInfor=new TrainingSummary.TrainingSummaryDuration(trainingName, durationTraining);
		
		String monthName= trainingRequest.getTrainingDate().getMonth().toString();
		List<TrainingSummary.Month> listTrainingMonth=new ArrayList<>();
		listTrainingMonth.add(new TrainingSummary.Month(monthName,List.of(trainingSummaryInfor)));
		
		int year=trainingRequest.getTrainingDate().getYear();
		List<TrainingSummary.Year> listTrainingYear=new ArrayList<>();
		listTrainingYear.add(new TrainingSummary.Year(year,listTrainingMonth));
		
		trainingSummary.setYears(listTrainingYear);
		dynamoDBMapper.save(trainingSummary);		
	}
	
	public void updateTraining(TrainingRequest trainingRequest) {
		//1. Retrieve the Trainer from DynamoDB
		TrainingSummary trainingSummary=existTraining(trainingRequest.getTrainerUsername()).get();
		
		//1. Values New from the request.
		int year=trainingRequest.getTrainingDate().getYear();
		String month= trainingRequest.getTrainingDate().getMonth().toString();
		String trainingName=trainingRequest.getTrainingTypeEnum();
		double trainingDuration=trainingRequest.getTrainingDuration();
		
		Optional<TrainingSummary.Year> existingYear=existingYear(trainingSummary, year);
		
		//2. Chequeos from dynamodb
		//2.1 If year exist, month exists?
		if(existingYear.isPresent()) {
			//2.2 If Month exist, training exists??
			Optional<TrainingSummary.Month> existingMonth=existingMonth(trainingSummary, existingYear, month);
			if(existingMonth.isPresent()) {
				//2.3 If training exist, increment the duration.
				Optional<TrainingSummary.TrainingSummaryDuration> existingTraining=existingTraining(existingMonth,trainingName);
				if(existingTraining.isPresent()) {
					//Increment the duration
					existingTraining.get().setDuration(existingTraining.get().getDuration()+trainingDuration);
				}else {
					TrainingSummary.TrainingSummaryDuration newTrainingDuration=new TrainingSummary.TrainingSummaryDuration(trainingName, trainingDuration);
				    existingMonth.get().getTrainingSummaryDuration().add(newTrainingDuration);
					}
			}else {
				existingYear.get().getMonths().add(new TrainingSummary.Month(month, List.of(new TrainingSummary.TrainingSummaryDuration(trainingName, trainingDuration))));
			}
		}else {
			TrainingSummary.Year newYear=new TrainingSummary.Year(year,List.of(
					new TrainingSummary.Month(month,
							List.of(new TrainingSummary.TrainingSummaryDuration(trainingName, trainingDuration)))));
			trainingSummary.getYears().add(newYear);
		}
		dynamoDBMapper.save(trainingSummary);		
	}
	
	public Optional<TrainingSummary> existTraining(String username) {
		TrainingSummary existingTraining=dynamoDBMapper.load(TrainingSummary.class, username);
		if(existingTraining==null) {
			return Optional.empty();
		}else {
			return Optional.of(existingTraining);
		}
	}

	public void deleteTraining(TrainingRequest trainingRequest) {
		Optional<TrainingSummary> existingTraining=existTraining(trainingRequest.getTrainerUsername());
		
		String month= trainingRequest.getTrainingDate().getMonth().toString();
		String trainingName=trainingRequest.getTrainingTypeEnum();
		double trainingDuration=trainingRequest.getTrainingDuration();
		int year=trainingRequest.getTrainingDate().getYear();
		
		if(!existingTraining.isPresent()) {
			throw new RuntimeException("Training not found for trainer "+trainingRequest.getTrainerUsername());
		}

		boolean existTraining=existingTraining.get().getYears().stream().flatMap(yearNumber->yearNumber.getMonths().stream())
				.flatMap(monthN->monthN.getTrainingSummaryDuration().stream())
				.anyMatch(n->n.getTrainingName().equals(trainingRequest.getTrainingTypeEnum()));
		
		if(!existTraining) throw new ResourceNotFoundException("Training "+ trainingRequest.getTrainingTypeEnum() +" wasnt found");
		
		Optional<TrainingSummary.Year> existingYear=existingYear(existingTraining.get(), year);
		if(existingYear.isPresent()) {
			Optional<TrainingSummary.Month> existingMonth=existingMonth(existingTraining.get(), existingYear, month);
			if(existingMonth.isPresent()) {
				Optional<TrainingSummary.TrainingSummaryDuration> existingTrainingSumary=existingTraining(existingMonth,trainingName);
				if(existingTrainingSumary.isPresent()) {
					if((existingTrainingSumary.get().getDuration()-trainingDuration)<0) {
						throw new RuntimeException("Existing hours: "+existingTrainingSumary.get().getDuration()+" trying to delete: "+trainingDuration+" hours");
					}else {
						existingTrainingSumary.get().setDuration(existingTrainingSumary.get().getDuration()-trainingDuration);
					}
				}else {
					throw new ResourceNotFoundException("Trying to delete a non existing Training in year: "+month);
					}
			}else {
				throw new ResourceNotFoundException("Trying to delete a non existing Month in year: "+month);
			}
		}else {
			throw new ResourceNotFoundException("Trying to delete a non existing year: "+year);
			}
		dynamoDBMapper.save(existingTraining.get());		

	}
	
	public Optional<TrainingSummary.Year> existingYear(TrainingSummary trainingSummary,int yearNumber){
			return trainingSummary.getYears().stream()
					.filter(year->year.getYearNumber()==yearNumber)
					.findFirst();}
	
	public Optional<TrainingSummary.Month> existingMonth(TrainingSummary trainingSummary,Optional<TrainingSummary.Year> existingYear,String monthName){
		return existingYear.get().getMonths().stream()
				.filter(month->month.getName().equals(monthName))
				.findFirst();}
	
	public Optional<TrainingSummary.TrainingSummaryDuration> existingTraining(Optional<TrainingSummary.Month> existingMonth,String trainingName){
	    return existingMonth.flatMap(month -> {
	       List<TrainingSummary.TrainingSummaryDuration> durations = month.getTrainingSummaryDuration();
	        if (durations != null) {
	        	Optional<TrainingSummary.TrainingSummaryDuration> foundDuration=durations.stream()
	        			.filter(duration->duration.getTrainingName().equals(trainingName))
	        			.findFirst();
	        	return foundDuration;
	        } else {
	        	return Optional.empty();
	        }
	    });
	    		
	
	}
	
}










