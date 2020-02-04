package jp.wings.nikkeibp.realtimechat

 open class Messagemodel()  {
    var userName: String = "" // ユーザー名
    var userPhotoUrl: String = "" // ユーザー写真URL
    var postMessage: String = "" // 投稿メッセージ
    var postImageUrl: String = "" //投稿画像URL
    // チャットのデータを保存するためのデータベースを確保している
    //条件としてデフォルトコンストラクタにする必要がある(Messagemode()にする),getValueを使うために()にする
    //セカンダリーコンストラクタが必要である。
    constructor(
         userName: String ,
        userPhotoUrl: String,
         postMessage: String ,//ここでのthisはMessagemodelを表している。
         postImageUrl: String ): this(){
        this.userName = userName
        this.userPhotoUrl = userPhotoUrl
        this.postMessage = postMessage
        this.postImageUrl = postImageUrl

    }
}