package com.sungwoobook.ebook.Model;

import java.util.List;

public class DataModel {
    private List<ContentModel> contents;

    public DataModel(List<ContentModel> contents) {
        this.contents = contents;
    }

    public List<ContentModel> getContents() {
        return contents;
    }
}
