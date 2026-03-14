你现在看下 ea-service 和 ea-web ，
1. 我现在要加入 权限认证系统，
2. 整体使用spring-security 和 jwt 作为验证，
3. 前端登录后，后端返回携带相关的jwt的 cookie，借助于域名共享cookie，每次请求就会自动携带cookie
4. 我理解应该都是新增代码就行了，不涉及改动现有代码
5. 相关的表已经建好了，在 auth.sql
