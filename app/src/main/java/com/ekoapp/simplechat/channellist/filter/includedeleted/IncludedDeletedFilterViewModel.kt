package com.ekoapp.simplechat.channellist.filter.includedeleted

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IncludedDeletedFilterViewModel : ViewModel() {
    var isIncludedDeletedSelected: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply { this.value = true }
}