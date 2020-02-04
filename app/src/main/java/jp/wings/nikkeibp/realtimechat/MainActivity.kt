package jp.wings.nikkeibp.realtimechat

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    //　認証システム
  var firebaseAuth: FirebaseAuth? = null
    var firebaseUser: FirebaseUser? = null
    // ファイルを読み書きするのに必要
    var firebaseReference: DatabaseReference? = null
    var layoutManager: LinearLayoutManager? = null
    lateinit var firebaseAdapter: FirebaseRecyclerAdapter<Messagemodel,MessageHolder>

   var userName : String = ""
    var userPhotoUrl :String = ""

lateinit var mGoogleSignInClient: GoogleSignInClient





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        registernoification()


//  Drawerとtoolbarの連携を取ってくれている。
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        logIncheck()
        //referenceの取得　データベースの読み書きに必要
        firebaseReference = FirebaseDatabase.getInstance().reference
        displaychat()

        //　オプションを設定している サインアウトするために必要？
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //Token ひとつずきの文字列
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // googleのアクセス権を取得している。
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        buttonsendMS.setOnClickListener {
            postMessage()
        }
        buttonaddPhto.setOnClickListener {
            postImage()
        }
    }

    private fun registernoification() {
        // 通知チャネルの設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Channel1"
            val descriptionText = "新規にメッセージがあった場合に通知を表示します。"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun displaychat() {
        //　layoutManagerの設定
        layoutManager = LinearLayoutManager(this)
        layoutManager!!.stackFromEnd = true //chatを投稿するときにcardviewを下から上にメッセージを投稿してくれる
        chatlist.layoutManager = layoutManager
        //query 問い合わせの意味
        val query = firebaseReference!!.child(MY_CHAT_TBL).limitToLast(50)
        val option = FirebaseRecyclerOptions.Builder<Messagemodel>()
            .setQuery(query, Messagemodel::class.java)
            .build()
        firebaseAdapter = object : FirebaseRecyclerAdapter<Messagemodel,MessageHolder>(option){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_content,parent,false)
                return MessageHolder(view)

            }

            override fun onBindViewHolder(holder: MessageHolder?, position: Int, model: Messagemodel?) {
                //　チャットデータのセット
                setUserContent(holder!!,model!!)
                setChatContent(holder,model)

            }
        }
         chatlist.adapter = firebaseAdapter
        //registerAdapterDataObserverはlistViewが変わるかどうか聞いているsetOnClickListenerに似ている
        firebaseAdapter.registerAdapterDataObserver(object :RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val chatMessageCount = firebaseAdapter.itemCount//チャットのリストが何行あるのか
                val lastvisiblePosition = layoutManager!!.findLastCompletelyVisibleItemPosition()//最後の行を知るため
                if (lastvisiblePosition == -1 || positionStart >= chatMessageCount -1
                    && lastvisiblePosition == positionStart -1){
                    chatlist.scrollToPosition(positionStart)
                }
            }
        })
        //　スマホに通知を送る firebaseReferenceはデータベースを参照するためのクラス
        // child で次の階層に進む　addChildEventListenerはメッセージが追加されたらそれだけを持ってくる。
        //　value はすべてのデータを持ってきてしまうのでaddChildEventListenerを持ってきた。
        firebaseReference!!.child(MY_CHAT_TBL).addChildEventListener(object : ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }
                //　通知メッセージを出すためのもの（onChildAdded）
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    // snapshotはある一つのデータ
                    val newMessage = snapshot.getValue(Messagemodel::class.java)
                    // newMessage!!.userNameのメッセージが自分のものではない場合、
                    if(newMessage!!.userName != userName) sendNofitication(newMessage!!)

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }

    private fun sendNofitication(newMessage: Messagemodel) {
 val notificationId  = SEND_NOTIFICATION_ID
 val pendingIntent = PendingIntent.getActivity(
     this@MainActivity,
     REQUEST_PENDING_INTENT,
     Intent(this@MainActivity, MainActivity::class.java),
     PendingIntent.FLAG_UPDATE_CURRENT
 )          //　notificationBuilderを使って、通知の設定を作成
        val notificationBuilder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_account_circle_black_24dp)
            .setContentTitle(newMessage.userName)
            .setContentText(newMessage.postMessage)
            .setAutoCancel(true)
        notificationBuilder.setContentIntent(pendingIntent)
        // .buildで　通知を作成
        val notification = notificationBuilder. build()
        notification.flags = Notification.DEFAULT_LIGHTS or Notification.FLAG_AUTO_CANCEL
        val notificationManager  = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId,notification)
        //通知が送れるようになる
    }


    private fun setChatContent(holder: MessageHolder, model: Messagemodel) {
        // textのchatのデータや画像ファイルを取ってくる。
        if (model.postMessage != ""){
            holder.apply {
                text_posted.text = model.postMessage
                text_posted.visibility = View.VISIBLE //メッセージを見えるようにする
                image_posted.visibility = View.GONE

            }
            return
        }
        setImageContent(holder,model)
    }

    private fun setImageContent(holder: MessageHolder, model: Messagemodel) {
        //modelのfirebaseからurlを取ってくる。画像をダウンロードしている

        val imageUri = model.postImageUrl
        holder.apply {
            text_posted.visibility = View.INVISIBLE
            image_posted.visibility = View.VISIBLE

        }
        if(!imageUri.startsWith("gs://")){//gsというのはfirebaseのstorageにある画像データgs://から始まるurl
            Glide.with(holder.image_posted.context)
                .load(model.postImageUrl)
                .into(holder.image_posted)
            return
        }
        // 画像のデータをストレージから取ってきている
        val storageRef  =FirebaseStorage.getInstance().getReferenceFromUrl(imageUri)
        storageRef.downloadUrl.addOnCompleteListener { task ->
            if (!task.isSuccessful){
                makeToast(this@MainActivity,getString(R.string.connection_failed))
                return@addOnCompleteListener
            }
            val downloadUrl = task.result
            Glide.with(holder.image_posted.context)
                .load(downloadUrl)
                .into(holder.image_posted)

        }
    }

    private fun setUserContent(holder: MessageHolder, model: Messagemodel) {
        // 一覧表示をしている。
        //Glideで画像を張り付けている。
holder.text_user_name.text = model.userName
        if (model.userPhotoUrl !=""){
            Glide.with(this)
                .load(Uri.parse(model.userPhotoUrl))
                .into(holder.image_user)
            return

        }
        holder.image_user
                    //setImageDrawableの方が処理の遅延がない
            .setImageDrawable(ContextCompat.getDrawable(this@MainActivity,R.drawable.ic_account_circle_black_24dp))
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter.startListening()
    }

    override fun onPause() {
        super.onPause()
        firebaseAdapter.stopListening()
    }

    private fun postImage() {//カメラギャラリーに移動している
        //　ACTION_OPEN_DOCUMENTは　minSdkVersion１９以降しか使えない 端末内の他のアプリからファイルを取得する
        //contentProviderの代わりにACTION_OPEN_DOCUMENTを使用。contentスキームをもってこれる
        // uri　ファイルの場所を示す書き方
        //暗黙的インテントでほかのアプリの力を借りる。
   val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
       addCategory(Intent.CATEGORY_OPENABLE)// 条件を絞っています。取るファイルを
       //ファイル名の拡張子をそのままでは理解できないwebサーバーに ファイルの種類を伝えるための書式規格　　画像全般は"image/*　なくてもいい
       type = "image/*"
   }
        startActivityForResult(intent, REQUEST_GET_IMAGE)
    }

    private fun postMessage() {
        // エラーの可能性ありinputMessageのところ　chatに表示させるもの　push().setValue(model)でDBに登録している
        val model = Messagemodel(userName,userPhotoUrl,inputMessage.text.toString(),"")
        firebaseReference!!.child(MY_CHAT_TBL).push().setValue(model)//MY_CHAT_TBLはテーブル名でConstans.ktで管理している
        inputMessage.setText("")

    }

    private fun logIncheck() {// Gougleにログインしているかどうかをチェック
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if(firebaseUser == null){
            startActivity(Intent(this@MainActivity,SignInActivity::class.java))
            finish()
            return

        }
        // ログインしている場合
        // barにメアドと名前を出してやる headerに
        setUserprofiles(firebaseUser!!)


    }
 // ３階建てのレイアウトのヘッダーに名前とメアドを入れてやる　nav_header_main.xml
    private fun setUserprofiles(firebaseUser: FirebaseUser) {
        val nav_header = nav_view.getHeaderView(0)//ヘッダが一つしかないから配列は０で良い
        val textUserName = nav_header.findViewById<TextView>(R.id.text_user_name)
        val textUserEmail = nav_header.findViewById<TextView>(R.id.text_user_id)
        textUserName.text = firebaseUser.displayName
        textUserEmail.text = firebaseUser.email

        userName = firebaseUser.displayName!!
        userPhotoUrl = firebaseUser.photoUrl.toString()
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.nav_menu_sign_ont -> {
                // handle the camera action
                signOut()

            }
            R.id.nav_menu_invite -> {
                sendInvitation()


            }

        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()//firebaseのログアウト

        mGoogleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful){
                startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                finish()
                return@addOnCompleteListener
                
            }
        }


    }

    private fun sendInvitation() {
        //　招待メールの送信先を設定する画面をstartActivityForResultで開く
        val intent = AppInviteInvitation.IntentBuilder(getString(R.string.app_invite_title))
            .setMessage(getString(R.string.app_invite_message))
            .build()
        startActivityForResult(intent,
            REQUEST_INVITE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_INVITE -> invitationResult(requestCode,resultCode,data)
            REQUEST_GET_IMAGE -> getImageResult(resultCode,data)//contentスキームを取ってきているか、画像をdataに入っている
        }
    }

    private fun getImageResult(requestCode: Int, data: Intent?) {
        if (requestCode != Activity.RESULT_OK){
            makeToast(this@MainActivity,getString(R.string.get_image_failed))
            return
        }// dataが画面のデータのこと
        if (data == null) return
        val urlFromDevice = data.data!!
        //ここまでがuri(画像の)取得
        //画像はgoogleストレージに保存される。
        val tempMessage = Messagemodel(userName,userPhotoUrl,"","")
        //　push()でkeyを取得している
        firebaseReference!!.child(MY_CHAT_TBL).push().setValue(tempMessage){ databaseError, databaseReference ->
            if (databaseError != null){
                makeToast(this@MainActivity,getString(R.string.db_write_error))
                return@setValue
            }
            // firebaseのデータベースと画面を取り込むストレージを結びつけるkeyを取得
            val key = databaseReference.key!!
            // アップロードする場所を取ってきている。
            val storageRef = FirebaseStorage.getInstance().getReference(firebaseUser!!.uid)
                .child(key).child(urlFromDevice.lastPathSegment!!)
            putImageStorage(storageRef,urlFromDevice,key)
        }
    }
     //　firedatabaseに画像をいれてやる。
    private fun putImageStorage(storageRef: StorageReference, urlFromDevice: Uri, key: String) {
        storageRef.putFile(urlFromDevice).continueWithTask {
            task ->
            if (!task.isSuccessful){}
            return@continueWithTask storageRef.downloadUrl//downloadUrlが処理の結果
        }.addOnCompleteListener {
            task ->
            if (!task.isSuccessful){
                makeToast(this@MainActivity,getString(R.string.image_upload_error))
                return@addOnCompleteListener
            }
            //画像の投稿
            val chatMessage = Messagemodel(userName,userPhotoUrl,"",task.result.toString())//resultはdownloadUrlである
            firebaseReference!!.child(MY_CHAT_TBL).child(key).setValue(chatMessage)
        }
    }

    private fun invitationResult(requestCode: Int, resultCode: Int, data: Intent?) {
          if(requestCode != Activity.RESULT_OK){
              makeToast(this@MainActivity, getString(R.string.invitation_sent_error))
              return
          }

    }
}
