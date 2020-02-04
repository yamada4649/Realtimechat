package jp.wings.nikkeibp.realtimechat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.android.synthetic.main.content_signin.*


class SignInActivity : AppCompatActivity() {
     lateinit var mGoogleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        //　オプションを設定している　金メダルの6番目
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //Token ひとつずきの文字列
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        sign_in_button.setOnClickListener {
            signIn()
        }

    }
       // GoogleApiに
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent// signInIntentは暗黙的インテント　やりたいことを指定して起動対象はシステムにまかせ
           //自分以外のアプリの力を借りる時のIntentの能力
        startActivityForResult(signInIntent, REQUEST_SIGN_IN)//質問をこちらでもうスタンバイしておく。
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SIGN_IN){
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (!result.isSuccess){
                makeToast(this@SignInActivity,getString(R.string.google_sign_in_failed))
                return
            }
            val account = result.signInAccount
            firebaseAuthWithGoogle(account!!)
        }
    }
// Goobleに成功したアカウントをfirebaseで認証しているメソッド
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        //APIの決まりごと                                    //Token取得
        val credential = GoogleAuthProvider.getCredential(account.idToken,null) //token取得
        //　Firebaseの認証システムにログインして、googleアカウントのidTokenを使って、成功したかどうかをaddOnCompleteListenerに書く。
        //　別のコンベアで仕事をしている　addOnCompleteListenerで仕事が終わったかどうか聞いている
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task: Task<AuthResult> ->
            //認証できたらMainActivityに飛ぶようになっている
            if(!task.isSuccessful){
                makeToast(this@SignInActivity,getString(R.string.google_sign_in_failed))
                return@addOnCompleteListener

            }
            startActivity(Intent(this@SignInActivity,MainActivity::class.java))
            finish()

        }
    }

}
