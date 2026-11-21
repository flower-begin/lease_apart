// 登录（账号密码）
export interface loginQueryInterface {
  // 登录账号，例如 zhangsan
  username: string;
  // 密码
  password: string;
}
// 注册
export interface RegisterInterface {
  // 登录账号
  username: string;
  // 密码
  password: string;
  // 手机号
  phone: string;
  // 昵称（选填）
  nickname?: string;
}

// 获取短信验证码
export interface SmsCodeQueryInterface {
  // 手机号码
  phone: string;
}

// 用户信息
export interface UserInfoInterface {
  // 头像
  avatarUrl: string;
  // 用户名
  nickname: string;
  // 手机号
  phone?: string;
  // 角色：admin-管理员 tenant-租客
  roleCode?: string;
}
// 用户state
export interface UserStateInterface {
  // 用户信息
  userInfo: UserInfoInterface | null;
  // token
  token: string | null;
}
