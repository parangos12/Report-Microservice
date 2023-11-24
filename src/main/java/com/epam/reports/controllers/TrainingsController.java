package com.epam.reports.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.epam.reports.entities.TrainingRequest;
import com.epam.reports.service.TrainingSummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/report/trainings")
@RequiredArgsConstructor
public class TrainingsController {

	private final TrainingSummaryService trainingService;
	private final QueueMessagingTemplate queueMessagingTemplate;
	
	@Value("${amazon.sqs.endpoint}")
	private String sqsEndpoint;
	
	@GetMapping("/send/{message}")
	public void senMessageToQueue(@PathVariable String message) {
		queueMessagingTemplate.send(sqsEndpoint,MessageBuilder.withPayload(message).build());
	}
	
	@PostMapping
	public ResponseEntity<?> addTraining(@RequestBody TrainingRequest trainingRequest){
		if(trainingRequest.getActionType().equals("ADD")){
			this.trainingService.saveUpdateTraining(trainingRequest);
		}else if(trainingRequest.getActionType().equals("DELETE")) {
			this.trainingService.deleteTrainingSummary(trainingRequest);
		}else {
			throw new RuntimeException("Action Type undefined.");
		}
		return ResponseEntity.ok().build();
	}
	
}
