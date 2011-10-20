package org.junit.test.internal;

import java.util.List;

import org.junit.runner.Description;
import org.junit.test.EventCollector;

public class ReadStartedTestRunsCommand implements
		ReadEventsCommand<Description> {
	public String getName() {
		return "started test run";
	}

	public List<Description> getEventsFromEventCollector(
			EventCollector eventCollector) {
		return eventCollector.getStartedTestRuns();
	}
}
