package io.temporal.samples.batchprocessing;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BatchChildWorkflowImpl implements BatchChildWorkflow {

  private final ActivityOptions options =
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(60)).build();
  private final BatchActivities activities =
          Workflow.newActivityStub(BatchActivities.class, options);

  @Override
  public void processBatch(List<String> batch) {
    // Example: split the batch into two sublists for list one and list two
    int midpoint = batch.size() / 2;
    List<String> oneBatch = batch.subList(0, midpoint);
    List<String> twoBatch = batch.subList(midpoint, batch.size());

    List<Promise<String>> onePromiseList = new ArrayList<>();
    List<Promise<String>> twoPromiseList = new ArrayList<>();

    // batch one promises
    for (String item : oneBatch) {
      onePromiseList.add(
              Async.function(activities::processRecordAAA, item)
      );
    }

    // batch two promises
    for (String item : twoBatch) {
      twoPromiseList.add(
              Async.function(activities::processRecordBBB, item)
      );
    }

    // combined promises
    Promise<Void> doneOne = Promise.allOf(onePromiseList);
    Promise<Void> doneTwo = Promise.allOf(twoPromiseList);

    System.out.println("Waiting for both batches to complete");
    Promise.allOf(doneOne, doneTwo).get();

  }
}