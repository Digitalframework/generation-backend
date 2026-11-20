package com.image.generation.impl;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;
import com.image.generation.common.Sorts;
import com.image.generation.repository.CustomFurnitureImageRepository;
import com.image.generation.entity.FurnitureImageContent;
import com.image.generation.entity.FurnitureImageFile;
import com.image.generation.entity.Room;
import com.image.generation.entity.FurnitureCategory;
import com.image.generation.entity.FurnitureImage;
import com.image.generation.repository.FurnitureImageRepository;
import com.image.generation.request.ImageMetadataRequest;
import com.image.generation.request.ImageUploadForm;
import com.image.generation.request.ImageUploadRequest;
import com.image.generation.response.FurnitureImageResponse;
import com.image.generation.storage.ImageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * The four methods that change the catalogue require ADMIN; the three that read it
 * do not.
 *
 * ImageController states the same rule on its endpoints, and this is not merely a
 * repetition of it: that one guards the HTTP door, this one guards the method, so a
 * caller arriving any other way — a scheduled job, an importer, a controller added
 * next month — is held to it too. The two are worth keeping in step by hand.
 *
 * The check is applied by a proxy, which means it only fires on calls that arrive
 * from outside this class. {@code create} calling {@code get} internally is not
 * re-checked, which is right here — but it is also why a future private helper must
 * not become the only place a rule is written.
 */
@Service
@Transactional(readOnly = true)
public class ImageService
        implements ICrudBusiness<ImageUploadForm, ImageMetadataRequest, FurnitureImageResponse> {

    private final FurnitureImageRepository images;
    private final CategoryService categories;
    private final ImageUpload imageUpload;
    private final ImageStorage storage;

    ImageService(FurnitureImageRepository images, CategoryService categories, ImageUpload imageUpload,
                 ImageStorage storage) {
        this.images = images;
        this.categories = categories;
        this.imageUpload = imageUpload;
        this.storage = storage;
    }

    public Page<FurnitureImageResponse> search(Long categoryId, Room room, String style, Pageable pageable) {
        return images.search(categoryId, room, style,
                Sorts.clean(pageable, CustomFurnitureImageRepository.SORTABLE, CustomFurnitureImageRepository.DEFAULT_ORDER));
    }

    @Override
    public FurnitureImageResponse get(Long id) throws NotFoundException {
        return images.findResponseById(id).orElseThrow(() -> NotFoundException.of("Image", id));
    }

    /**
     * Metadata for a known set of ids in one query. Ids that do not exist are left out
     * of the result rather than raising {@link NotFoundException} — a batch lookup, not
     * a series of individual {@link #get} calls.
     */
    public List<FurnitureImageResponse> get(List<Long> ids) {
        return images.findResponsesByIdIn(ids);
    }

    /**
     * Reads the picture back out of the active {@link ImageStorage}, together with the
     * content type it was stored under, so it can be streamed back as whatever format
     * it arrived in.
     */
    public FurnitureImageContent content(Long id) throws NotFoundException {
        FurnitureImageFile file = images.findFileById(id).orElseThrow(() -> NotFoundException.of("Image", id));
        return new FurnitureImageContent(file.fileName(), file.contentType(), file.sha256(),
                storage.load(file.sha256()));
    }

    /**
     * The picture and the three fields chosen alongside it arrive as one form, which is
     * what makes an upload fit the shape {@link ICrudBusiness} asks of a create.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public FurnitureImageResponse create(ImageUploadForm form) throws NotFoundException, ConflictException {
        return create(form.file(), form.toRequest());
    }

    /**
     * The name and, if the file name states them, the measurements are read from the
     * upload file name ({@code Hoeffner_1_180_20_3.png}); room, category and style are
     * picked in the form. A name without measurements is stored with none.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FurnitureImageResponse create(MultipartFile file, ImageUploadRequest request)
            throws NotFoundException, ConflictException {
        ImageUpload.UploadedImage uploaded = imageUpload.read(file);
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(uploaded.fileName());
        rejectDuplicate(uploaded.sha256(), null);

        FurnitureCategory category = categories.require(request.room(), request.category());
        FurnitureImage image = new FurnitureImage(
                category,
                parsed.name(),
                uploaded.fileName(),
                uploaded.contentType(),
                uploaded.widthPx(),
                uploaded.heightPx(),
                uploaded.data().length,
                uploaded.sha256(),
                parsed.widthCm(),
                parsed.heightCm(),
                parsed.depthCm(),
                request.style().trim());

        Long id = images.save(image).getId();
        // Flushed first, so a constraint violation stops the upload before anything is stored.
        images.flush();
        storage.save(uploaded.sha256(), uploaded.contentType(), uploaded.data());
        deleteFromStorageOnRollback(uploaded.sha256());
        return get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public FurnitureImageResponse update(Long id, ImageMetadataRequest metadata) throws NotFoundException {
        FurnitureImage image = require(id);
        image.setCategory(categories.require(metadata.categoryId()));
        image.setName(metadata.name().trim());
        image.setWidthCm(metadata.widthCm());
        image.setHeightCm(metadata.heightCm());
        image.setDepthCm(metadata.depthCm());
        image.setStyle(metadata.style().trim());
        images.flush();
        return get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FurnitureImageResponse replaceContent(Long id, MultipartFile file)
            throws NotFoundException, ConflictException {
        FurnitureImage image = require(id);
        ImageUpload.UploadedImage uploaded = imageUpload.read(file);
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(uploaded.fileName());
        rejectDuplicate(uploaded.sha256(), id);
        String previousSha256 = image.getSha256();
        image.replaceContent(parsed, uploaded.fileName(), uploaded.contentType(),
                uploaded.widthPx(), uploaded.heightPx(), uploaded.data().length, uploaded.sha256());
        images.flush();
        storage.save(uploaded.sha256(), uploaded.contentType(), uploaded.data());
        if (!previousSha256.equals(uploaded.sha256())) {
            deleteFromStorageOnRollback(uploaded.sha256());
            deleteFromStorageAfterCommit(previousSha256);
        }
        return get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(Long id) throws NotFoundException {
        FurnitureImage image = require(id);
        images.delete(image);
        deleteFromStorageAfterCommit(image.getSha256());
    }

    /**
     * A bucket is not part of the database transaction. Bytes written for a row that
     * then rolls back are removed again, so no orphaned object is left behind.
     */
    private void deleteFromStorageOnRollback(String sha256) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storage.delete(sha256);
                }
            }
        });
    }

    /**
     * Old bytes are only removed once the row change is committed, so a rollback never
     * leaves a row pointing at a picture that is already gone.
     */
    private void deleteFromStorageAfterCommit(String sha256) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(sha256);
            }
        });
    }

    private FurnitureImage require(Long id) throws NotFoundException {
        return images.findById(id).orElseThrow(() -> NotFoundException.of("Image", id));
    }

    /**
     * An {@code ifPresent} lambda cannot carry a checked exception out, so the optional is
     * unwrapped here first and the throw made in ordinary code.
     */
    private void rejectDuplicate(String sha256, Long allowedId) throws ConflictException {
        Optional<FurnitureImage> duplicate = images.findBySha256(sha256)
                .filter(existing -> !existing.getId().equals(allowedId));
        if (duplicate.isPresent()) {
            throw new ConflictException("This exact image is already stored as image " + duplicate.get().getId());
        }
    }
}
