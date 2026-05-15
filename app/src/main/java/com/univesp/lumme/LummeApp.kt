package com.univesp.lumme

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Ponto de entrada da aplicação. A anotação [HiltAndroidApp] gera
 * o componente Dagger-Hilt raiz que hospeda todas as dependências.
 */
@HiltAndroidApp
class LummeApp : Application()
