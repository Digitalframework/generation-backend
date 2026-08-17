package com.image.generation.impl;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;

/**
 * The four operations every catalogue service offers on a single row, named the same
 * way in each of them so a caller — or a reader — does not have to learn two vocabularies.
 *
 * The request types are separate because creating and updating are rarely told the same
 * things: a category is described identically either way, while an image is created from
 * an uploaded file and later corrected by hand. Reads that are particular to one service
 * (listing, searching, lookup by slug, streaming the blob) stay off this interface; only
 * what genuinely recurs belongs here.
 *
 * The exceptions are declared at their widest here so that either kind may be thrown by
 * an implementation; one that cannot conflict simply narrows its own {@code throws}.
 *
 * @param <C> what a create is given
 * @param <U> what an update is given
 * @param <R> what is handed back, i.e. the response DTO
 */
public interface ICrudBusiness<C, U, R> {

    R create(C request) throws NotFoundException, ConflictException;

    R get(Long id) throws NotFoundException;

    R update(Long id, U request) throws NotFoundException, ConflictException;

    void delete(Long id) throws NotFoundException, ConflictException;
}
