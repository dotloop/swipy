package com.dotloop.sweepy.app

interface ActionListener {
    fun onDeleteClicked()
    fun onArchiveClicked()
    fun onEmailClicked()
    fun onPhoneClicked()
    fun onTxtMessageClicked()
}