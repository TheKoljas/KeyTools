package com.example.keytools.ui.keygrab;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class KeyGrabViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public KeyGrabViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}