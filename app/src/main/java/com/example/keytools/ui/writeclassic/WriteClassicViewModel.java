package com.example.keytools.ui.writeclassic;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WriteClassicViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public WriteClassicViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}