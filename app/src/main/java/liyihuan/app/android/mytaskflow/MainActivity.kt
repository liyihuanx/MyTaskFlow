package liyihuan.app.android.mytaskflow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.liyihuanx.module_taskflow.TaskStartUp
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvTaskFlow.setOnClickListener {
            TaskStartUp.start()
        }
    }
}