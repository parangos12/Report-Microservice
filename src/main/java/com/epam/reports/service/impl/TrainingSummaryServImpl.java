package com.epam.reports.service.impl;

import org.springframework.stereotype.Service;

import com.epam.reports.entities.TrainingRequest;
import com.epam.reports.repositories.TrainingRepository;
import com.epam.reports.service.TrainingSummaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainingSummaryServImpl implements TrainingSummaryService{

	private final TrainingRepository trainingRepository;
	
	@Override
	public void saveUpdateTraining(TrainingRequest TrainingRequest) {
		trainingRepository.saveUpdateTraining(TrainingRequest);
	}

	@Override
	public void deleteTrainingSummary(TrainingRequest TrainingRequest) {
		trainingRepository.deleteTraining(TrainingRequest);		
	}

}
