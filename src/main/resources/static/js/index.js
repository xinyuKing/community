$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");
	// 获取标题内容
	var title=$("#recipient-name").val();
	var content=$("#message-text").val();
	// 发送ajax请求
	$.post(
		"/community/discuss/add",
		{"title":title,"content":content},
		function (data) {
			data=$.parseJSON(data);
			//在提示框中显示返回的消息
			$("#hintBody").text(data.msg);
			//显示提示框
			$("#hintModal").modal("show");
			//2秒后自动隐藏
			setTimeout(function(){
				$("#hintModal").modal("hide");
				//成功时刷新页面
				if(data.code==0){
					window.location.reload();
				}
			}, 2000);
		}
	)


}