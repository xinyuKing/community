$(function(){
    $("#uploadForm").submit(upload);
    $("#updatePassword").submit(updatePassword);
});

function showAlert(msg) {
    alert(msg); // 显示Alert

    setTimeout(function() {
        close(); // 关闭当前窗口
    }, 3000); // 3秒后执行关闭操作
}

function updatePassword() {
    var oldPassword=$("#old-password").val();
    var newPassword=$("#new-password").val();
    $.post(
        CONTEXT_PATH+"/user/setting/updatepassword",
        {"oldPassword":oldPassword,"newPassword":newPassword},
        function (data) {
            data=$.parseJSON(data);
            if(data.code==0){

                showAlert("登录成功");
                /*退出，重新登录*/
                location.href=CONTEXT_PATH+"/logout";
            }else{
                showAlert(data.msg);
            }
        }
    )
}


function upload() {
    $.ajax({
        url:"http://upload-cn-east-2.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function (data) {
            if(data&&data.code==0){
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH+"/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function (data) {
                        data=$.parseJSON(data);
                        if (data.code==0){
                            window.location.reload();
                        }else{
                            alert(data.msg);
                        }
                    }
                )
            }else{
                alert("上传失败！")
            }
        }
    });
    return false;
}