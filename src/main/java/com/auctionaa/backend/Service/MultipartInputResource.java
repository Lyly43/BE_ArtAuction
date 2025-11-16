package com.auctionaa.backend.Service;

import org.springframework.core.io.AbstractResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class MultipartInputResource extends AbstractResource {

    private final MultipartFile file;

    public MultipartInputResource(MultipartFile file) {
        this.file = file;
    }

    @Override
    public String getDescription() {
        return "MultipartInputResource for file: " + file.getOriginalFilename();
    }

    @Override
    public String getFilename() {
        return file.getOriginalFilename();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return file.getInputStream();
    }

    @Override
    public long contentLength() throws IOException {
        return file.getSize();
    }
}
