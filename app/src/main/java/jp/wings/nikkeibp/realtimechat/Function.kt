package jp.wings.nikkeibp.realtimechat

import android.content.Context
import android.widget.Toast
//どのクラスからにもアクセスできる
fun makeToast(content: Context, message: String){
    Toast.makeText(content,message, Toast.LENGTH_SHORT).show()
}