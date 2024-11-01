#!/bin/bash

# 控制台输出样式
BOLD=$(tput bold)
NORMAL=$(tput sgr0)
BLUE='\033[1;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'

# 显示信息的函数
show() {
    case $2 in
        "error")
            echo -e "${RED}${BOLD}❌ $1${NORMAL}"
            ;;
        "progress")
            echo -e "${YELLOW}${BOLD}⏳ $1${NORMAL}"
            ;;
        *)
            echo -e "${GREEN}${BOLD}✅ $1${NORMAL}"
            ;;
    esac
}

# 设置脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit

# 安装依赖的函数
install_dependencies() {
    show "初始化 Git 仓库..." "progress"
    if [ ! -d ".git" ]; then
        git init
    fi

    if ! command -v forge &> /dev/null; then
        show "未检测到 Foundry，正在安装..." "progress"
        source <(curl -sL https://foundry.paradigm.xyz)
    fi

    if [ ! -d "$SCRIPT_DIR/lib/openzeppelin-contracts" ]; then
        show "克隆 OpenZeppelin 合约库..." "progress"
        git clone https://github.com/OpenZeppelin/openzeppelin-contracts.git "$SCRIPT_DIR/lib/openzeppelin-contracts"
    else
        show "OpenZeppelin 合约库已存在。"
    fi
}

# 输入部署所需信息的函数
input_required_details() {
    echo -e "-----------------------------------"
    if [ -f "$SCRIPT_DIR/deployment/.env" ]; then
        rm "$SCRIPT_DIR/deployment/.env"
    fi

    read -p "请输入私钥: " PRIVATE_KEY
    read -p "请输入代币名称 (例如: MyToken): " TOKEN_NAME
    read -p "请输入代币符号 (例如: MTK): " TOKEN_SYMBOL
    read -p "请输入 RPC URL: " RPC_URL

    mkdir -p "$SCRIPT_DIR/deployment"
    cat <<EOL > "$SCRIPT_DIR/deployment/.env"
PRIVATE_KEY="$PRIVATE_KEY"
TOKEN_NAME="$TOKEN_NAME"
TOKEN_SYMBOL="$TOKEN_SYMBOL"
EOL

    # 生成配置文件
    cat <<EOL > "$SCRIPT_DIR/foundry.toml"
[profile.default]
src = "src"
out = "out"
libs = ["lib"]

[rpc_endpoints]
rpc_url = "$RPC_URL"
EOL

    show "文件已更新。"
}

# 合约部署函数
deploy_contract() {
    echo -e "-----------------------------------"
    source "$SCRIPT_DIR/deployment/.env"

    # 创建合约代码
    mkdir -p "$SCRIPT_DIR/src"
    cat <<EOL > "$SCRIPT_DIR/src/MyToken.sol"
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract MyToken is ERC20 {
    constructor() ERC20("$TOKEN_NAME", "$TOKEN_SYMBOL") {
        _mint(msg.sender, 100000 * (10 ** decimals()));
    }
}
EOL

    # 编译合约
    show "正在编译合约..." "progress"
    forge build

    if [[ $? -ne 0 ]]; then
        show "合约编译失败。" "error"
        exit 1
    fi

    # 部署合约
    show "正在部署合约..." "progress"
    DEPLOY_OUTPUT=$(forge create "$SCRIPT_DIR/src/MyToken.sol:MyToken" \
        --rpc-url rpc_url \
        --private-key "$PRIVATE_KEY")

    if [[ $? -ne 0 ]]; then
        show "合约部署失败。" "error"
        exit 1
    fi

    CONTRACT_ADDRESS=$(echo "$DEPLOY_OUTPUT" | grep -oP 'Deployed to: \K(0x[a-fA-F0-9]{40})')
    show "合约已成功部署，地址为: $CONTRACT_ADDRESS"
}

# 多合约部署函数
deploy_multiple_contracts() {
    echo -e "-----------------------------------"
    read -p "请输入需要部署的合约数量: " NUM_CONTRACTS
    if [[ $NUM_CONTRACTS -lt 1 ]]; then
        show "合约数量无效。" "error"
        exit 1
    fi

    ORIGINAL_TOKEN_NAME=$TOKEN_NAME

    for (( i=1; i<=NUM_CONTRACTS; i++ ))
    do
        if [[ $i -gt 1 ]]; then
            RANDOM_SUFFIX=$(head /dev/urandom | tr -dc A-Z | head -c 2)
            TOKEN_NAME="${ORIGINAL_TOKEN_NAME}_${RANDOM_SUFFIX}"
        else
            TOKEN_NAME=$ORIGINAL_TOKEN_NAME
        fi
        deploy_contract "$i"
        echo -e "-----------------------------------"
    done
}

# 菜单显示函数
menu() {
    echo -e "\n${BLUE}┌─────────────────────────────────────┐${NORMAL}"
    echo -e "${BLUE}│           菜单选项                 │${NORMAL}"
    echo -e "${BLUE}├─────────────────────────────────────┤${NORMAL}"
    echo -e "${BLUE}│  1) 安装依赖                        │${NORMAL}"
    echo -e "${BLUE}│  2) 输入所需信息                    │${NORMAL}"
    echo -e "${BLUE}│  3) 部署合约                        │${NORMAL}"
    echo -e "${BLUE}│  4) 批量部署合约                    │${NORMAL}"
    echo -e "${BLUE}│  5) 退出                            │${NORMAL}"
    echo -e "${BLUE}└─────────────────────────────────────┘${NORMAL}"

    read -p "请输入选择 (1-5): " CHOICE

    case $CHOICE in
        1)
            install_dependencies
            ;;
        2)
            input_required_details
            ;;
        3)
            deploy_contract
            ;;
        4)
            deploy_multiple_contracts
            ;;
        5)
            exit 0
            ;;
        *)
            show "无效选择，请输入 1 到 5 之间的数字。" "error"
            ;;
    esac
}

# 主循环
while true; do
    menu
done
