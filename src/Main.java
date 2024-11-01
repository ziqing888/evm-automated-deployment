import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {

    private static String privateKey = "";
    private static String tokenName = "";
    private static String tokenSymbol = "";
    private static String rpcUrl = "";

    public static void main(String[] args) throws Exception {
        // 检查并安装依赖工具
        checkAndInstallDependencies();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("请选择操作:");
            System.out.println("1. 设置部署参数");
            System.out.println("2. 编译合约并生成 Java 类");
            System.out.println("3. 部署单个合约");
            System.out.println("4. 批量部署合约");
            System.out.println("5. 退出");

            System.out.print("请输入选项 (1-5): ");
            int choice = scanner.nextInt();
            scanner.nextLine();  // 清除换行符

            switch (choice) {
                case 1 -> setParameters(scanner);
                case 2 -> compileSolidityAndGenerateJava();
                case 3 -> deployContract(tokenName, tokenSymbol);
                case 4 -> deployMultipleContracts(scanner);
                case 5 -> {
                    System.out.println("退出程序。");
                    running = false;
                }
                default -> System.out.println("无效的选择，请输入 1-5 之间的数字。");
            }
        }
        scanner.close();
    }

    private static void checkAndInstallDependencies() throws Exception {
        System.out.println("⏳ 正在检查依赖工具...");

        // 检查 solc 是否已安装
        if (!isCommandAvailable("solc")) {
            System.out.println("❌ 未检测到 Solidity 编译器 (solc)，开始安装...");
            runCommand("curl -L https://raw.githubusercontent.com/ethereum/solidity/master/scripts/install_solc.sh | sh");
        } else {
            System.out.println("✅ Solidity 编译器已安装");
        }

        // 检查 web3j CLI 是否已安装
        if (!isCommandAvailable("web3j")) {
            System.out.println("❌ 未检测到 Web3j CLI，开始安装...");
            runCommand("curl -L get.web3j.io | sh");
        } else {
            System.out.println("✅ Web3j CLI 已安装");
        }
    }

    private static boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder("which", command).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void runCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        process.waitFor();
    }

    private static void setParameters(Scanner scanner) {
        System.out.print("请输入私钥: ");
        privateKey = scanner.nextLine();

        System.out.print("请输入代币名称: ");
        tokenName = scanner.nextLine();

        System.out.print("请输入代币符号: ");
        tokenSymbol = scanner.nextLine();

        System.out.print("请输入 RPC URL (例如: https://api.helium.fhenix.zone): ");
        rpcUrl = scanner.nextLine();

        System.out.println("✅ 部署参数已设置成功！");
    }

    private static void compileSolidityAndGenerateJava() throws Exception {
        System.out.println("⏳ 正在编译 Solidity 合约并生成 Java 类...");

        // 编译 Token.sol 合约
        ProcessBuilder solcProcess = new ProcessBuilder("solc", "--abi", "--bin", "contracts/Token.sol", "-o", "contracts/build");
        solcProcess.directory(new File(System.getProperty("user.dir")));
        Process compileProcess = solcProcess.start();
        compileProcess.waitFor();

        // 检查编译结果
        File abiFile = new File("contracts/build/Token.abi");
        File binFile = new File("contracts/build/Token.bin");
        if (!abiFile.exists() || !binFile.exists()) {
            System.out.println("❌ Solidity 合约编译失败，请检查 Token.sol 文件。");
            return;
        }

        // 生成 Token.java
        ProcessBuilder web3jProcess = new ProcessBuilder("web3j", "solidity", "generate",
                "-a", "contracts/build/Token.abi",
                "-b", "contracts/build/Token.bin",
                "-o", "src",
                "-p", "com.token");
        Process generateProcess = web3jProcess.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(generateProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        generateProcess.waitFor();

        File generatedJavaFile = new File("src/com/token/Token.java");
        if (generatedJavaFile.exists()) {
            System.out.println("✅ 成功生成 Token.java 文件。");
        } else {
            System.out.println("❌ 生成 Token.java 文件失败，请检查 Web3j 和合约文件。");
        }
    }

    private static void deployContract(String name, String symbol) throws Exception {
        if (privateKey.isEmpty() || rpcUrl.isEmpty()) {
            System.out.println("请先设置所有的部署参数！");
            return;
        }

        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        Credentials credentials = Credentials.create(privateKey);
        ContractGasProvider gasProvider = new DefaultGasProvider();

        System.out.println("⏳ 正在部署合约...");
        Token contract = Token.deploy(web3j, credentials, gasProvider, name, symbol).send();

        String contractAddress = contract.getContractAddress();
        System.out.println("✅ 合约 " + name + " 部署成功，地址为: " + contractAddress);
    }

    private static void deployMultipleContracts(Scanner scanner) throws Exception {
        System.out.print("请输入要部署的合约数量: ");
        int numContracts = scanner.nextInt();
        scanner.nextLine();  // 清除换行符

        if (numContracts < 1) {
            System.out.println("❌ 无效的合约数量。");
            return;
        }

        for (int i = 1; i <= numContracts; i++) {
            String uniqueName = tokenName + "_" + i;
            String uniqueSymbol = tokenSymbol + i;
            System.out.println("\n⏳ 部署合约 " + i + ": " + uniqueName + " (" + uniqueSymbol + ")");
            deployContract(uniqueName, uniqueSymbol);
        }
    }
}
