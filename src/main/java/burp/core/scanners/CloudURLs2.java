package burp.core.scanners;

import burp.*;
import burp.utils.Utilities;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.google.re2j.Matcher;

import static burp.utils.Constants.*;
import static burp.utils.Utilities.appendFoundMatches;
import static burp.utils.Utilities.sendNewIssue2;

public class CloudURLs2 implements Runnable {
    private static final IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
    private static final IExtensionHelpers helpers = callbacks.getHelpers();
    private final String baseRequestResponse;
    private final String url;
    private final UUID taskUUID;

    public CloudURLs2(String url, String baseRequestResponse, UUID taskUUID) {
        this.baseRequestResponse = baseRequestResponse;
        this.url = url;
        this.taskUUID = taskUUID;
    }

    @Override
    public void run() {
        BurpExtender.getTaskRepository().startTask(taskUUID);

        // For reporting unique matches with markers
        List<byte[]> uniqueMatches = new ArrayList<>();
        StringBuilder uniqueMatchesSB = new StringBuilder();
        String responseBodyString = baseRequestResponse;

        Matcher cloudURLsMatcher = CLOUD_URLS_REGEX.matcher(responseBodyString);

        while (cloudURLsMatcher.find() && BurpExtender.isLoaded()) {
            uniqueMatches.add(cloudURLsMatcher.group().getBytes(StandardCharsets.UTF_8));
            appendFoundMatches(cloudURLsMatcher.group(), uniqueMatchesSB);
        }

        reportFinding(url,baseRequestResponse, uniqueMatchesSB, uniqueMatches);

        BurpExtender.getTaskRepository().completeTask(taskUUID);

    }

    private static void reportFinding(String url, String baseRequestResponse, StringBuilder allMatchesSB, List<byte[]> uniqueMatches) {
        if (allMatchesSB.length() > 0) {
            // Get markers of found Cloud URL Matches
            List<int[]> allMatchesMarkers = Utilities.getMatches(helpers.stringToBytes(baseRequestResponse), uniqueMatches);

            // report the issue
            sendNewIssue2(url,
                    "[JS Miner] Cloud Resources",
                    "The following cloud URLs were found in a static file.",
                    allMatchesSB.toString(),
                    allMatchesMarkers,
                    SEVERITY_INFORMATION,
                    CONFIDENCE_CERTAIN
            );
        }
    }
}
