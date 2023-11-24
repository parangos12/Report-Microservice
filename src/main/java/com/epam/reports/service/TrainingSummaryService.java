package com.epam.reports.service;

import com.epam.reports.entities.TrainingRequest;

public interface TrainingSummaryService {

	
	void saveUpdateTraining(TrainingRequest TrainingRequest);
	void deleteTrainingSummary(TrainingRequest TrainingRequest);

}
