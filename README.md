# 使用说明
###  简介
此脚本功能包括单一合约部署和批量合约部署，脚本会自动完成所需环境的搭建以及所需的输入信息收集。

###  先决条件
Linux 或 macOS 系统。
已安装 Git。
已安装 curl，用于自动安装 Foundry。
确保终端支持 bash。
### 步骤

一键脚本命令
 ```bash
[ -f "deploy.sh" ] && rm deploy.sh; wget -q https://raw.githubusercontent.com/ziqing888/evm-automated-deployment/refs/heads/main/deploy.sh -O deploy.sh && chmod +x deploy.sh && ./deploy.sh

  ```
脚本启动后，会显示以下菜单选项：

1) 安装依赖
自动安装 Foundry 和克隆 OpenZeppelin 合约库（如尚未安装 Foundry，系统会自动安装）。

2) 输入所需信息
输入合约部署的私钥、代币名称、代币符号和 RPC URL。此步骤会生成 .env 文件和 foundry.toml 配置文件。

3) 部署合约
编译并部署合约，使用用户输入的配置。部署成功后会显示合约地址。

4) 批量部署合约
允许用户指定多个合约的部署数量，并以随机后缀生成不同的代币名称。

5) 退出
退出脚本。

详细说明
依赖安装


输入部署信息
会提示用户输入私钥、代币名称、代币符号和 RPC URL。这些信息会保存在 .env 文件中
单一和批量部署
合约代码会在 src/MyToken.sol 中创建，默认的合约为标准 ERC20 代币合约，名称和符号来自用户输入。批量部署会为每个代币名称附加随机后缀，以区分各合约。

注意事项
隐私保护
私钥将保存至 .env 文件中，请在完成部署后将其安全保存或删除，以防泄露。

