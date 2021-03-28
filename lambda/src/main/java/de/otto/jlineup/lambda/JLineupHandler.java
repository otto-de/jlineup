package de.otto.jlineup.lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class JLineupHandler implements RequestHandler<S3Event, String> {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        try {

            Jackson.getObjectMapper().registerModule(new JodaModule());
            LOG.info("Event: " + Jackson.getObjectMapper().writer().writeValueAsString(s3event));
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = record.getS3().getBucket().getName();
            String srcKey = record.getS3().getObject().getUrlDecodedKey();

            // Download the config from S3
            /*
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(
                    srcBucket, srcKey));
            InputStream objectData = s3Object.getObjectContent();
            */

            // Upload all created files to bucket
            /*
            LOG.info("Writing to: " + dstBucket + "/" + dstKey);
            try {
                s3Client.putObject(dstBucket, dstKey, is, meta);
            } catch (AmazonServiceException e) {
                LOG.error(e.getErrorMessage());
                System.exit(1);
            }
            */
            return "Ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}