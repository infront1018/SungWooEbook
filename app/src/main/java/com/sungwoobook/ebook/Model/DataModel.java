package com.sungwoobook.ebook.Model;

import java.util.List;

public class DataModel { // data 모델
    private List<ContentModel> contents;

    public DataModel(List<ContentModel> contents) {
        this.contents = contents;
    }

    public List<ContentModel> getContents() {
        return contents;
    }
}
