package az.mtk.app

import android.app.Application

class MtkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
