package uk.co.dotcode.customvillagertrades;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModLogger {

	private static final Logger manager = LogManager.getLogger(CVT.MOD_ID);
	private static final ArrayList<String> configIssues = new ArrayList<String>();

	public static void info(String message) {
		manager.log(Level.INFO, "CVT MESSAGE: " + message);
	}

	public static void warn(String message) {
		manager.log(Level.WARN, "CVT MESSAGE: " + message);
		configIssues.add(message);
	}

	public static void error(String message) {
		manager.log(Level.ERROR, "CVT MESSAGE: " + message);
		configIssues.add(message);
	}

	public static ArrayList<String> getConfigIssues() {
		Set<String> listWithoutDuplicates = new LinkedHashSet<String>(configIssues);
		configIssues.clear();
		configIssues.addAll(listWithoutDuplicates);

		return configIssues;
	}

	public static void clearConfigIssues() {
		configIssues.clear();
	}

}
