$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");


	/*注意：可以后续自己把所有的页面的异步都处理了，先再config中禁止csrf*/
	//发送AJAX请求之前，将CSRF令牌设置到请求的消息头中
	// var token=$("meta[name='_csrf']").attr("content");
	// var header=$("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e,xhr,options) {
	// 	xhr.setRequestHeader(header,token)
	// });

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