package com.dotloop.sweepy

interface ActionListener {
    fun onDeleteClicked()
    fun onArchiveClicked()
    fun onEmailClicked()
    fun onPhoneClicked()
    fun onTxtmessageClicked()
}