package com.image.generation.storage;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Keeps the bytes in a Google Cloud Storage bucket, one object per image under
 * {@code furniture/<sha256>}, with the content type set on the object.
 * <p>
 * With {@code app.storage.gcs.host} empty this talks to the real service and takes its
 * credentials from the environment (the Cloud Run service account, or locally
 * {@code gcloud auth application-default login}). With a host such as
 * {@code http://localhost:4443} it talks to fake-gcs-server without credentials and
 * creates the bucket if it does not exist yet.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "gcs")
class GcsImageStorage implements ImageStorage {

    private static final String PREFIX = "furniture/";

    private final Storage storage;
    private final String bucket;

    GcsImageStorage(@Value("${app.storage.gcs.bucket}") String bucket,
                    @Value("${app.storage.gcs.host:}") String host,
                    @Value("${app.storage.gcs.project-id:}") String projectId) {
        this.bucket = bucket;
        boolean emulator = !host.isBlank();

        StorageOptions.Builder options = StorageOptions.newBuilder();
        if (emulator) {
            options.setHost(host).setCredentials(NoCredentials.getInstance());
        }
        if (!projectId.isBlank()) {
            options.setProjectId(projectId);
        } else if (emulator) {
            options.setProjectId("local");
        }
        this.storage = options.build().getService();

        if (emulator && storage.get(bucket) == null) {
            storage.create(BucketInfo.of(bucket));
        }
    }

    @Override
    public void save(String sha256, String contentType, byte[] data) {
        BlobInfo info = BlobInfo.newBuilder(blobId(sha256)).setContentType(contentType).build();
        storage.create(info, data);
    }

    @Override
    public byte[] load(String sha256) {
        try {
            return storage.readAllBytes(blobId(sha256));
        } catch (StorageException e) {
            if (e.getCode() == 404) {
                throw new IllegalStateException("No object " + PREFIX + sha256 + " in bucket " + bucket, e);
            }
            throw e;
        }
    }

    @Override
    public void delete(String sha256) {
        storage.delete(blobId(sha256));
    }

    private BlobId blobId(String sha256) {
        return BlobId.of(bucket, PREFIX + sha256);
    }
}
