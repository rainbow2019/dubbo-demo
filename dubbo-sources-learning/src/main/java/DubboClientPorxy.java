import com.alibaba.fastjson.JSON;
import org.I0Itec.zkclient.ZkClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Random;

/**
 * 通过自己的代码，去访问dubbo服务提供者
 */
public class DubboClientPorxy {
    public static void main(String[] args) throws IOException {
        // userService.getUserNameById("123");
        // 1、 从Zookeeper注册中心获取有哪些服务实例。（不是每一次都获取）
        ZkClient zkClient = new ZkClient("localhost:2181");
        List<String> providers = zkClient.getChildren("/dubbo/com.dongnaoedu.tony.service.UserService/providers");
        System.out.println("查找到service有如下几个实例：");
        for (String provider : providers) {
            System.out.println(URLDecoder.decode(provider, "utf-8"));
        }
        // 2、 客户端负载均衡的过程：选择具体的服务实例（ip + port）
        // 负载策略：随机访问
        int index = new Random().nextInt(providers.size());
        String provider = URLDecoder.decode(providers.get(index), "utf-8");
        String[] ipPort = provider.split("/")[2].split(":");
        String host = ipPort[0];
        int port = Integer.valueOf(ipPort[1]);

        // 3、 发起请求
        SocketChannel dubboClient = SocketChannel.open();
        dubboClient.connect(new InetSocketAddress(host, port)); // 和服务器建立连接
        // 构建请求数据包。 header + body
        // -------------------------------组装请求报文 = header + body
        StringBuffer bodyString = new StringBuffer();
        // body
        // dubbo rpc版本
        bodyString.append(JSON.toJSONString("2.0.1")).append("\r\n");
        // 服务接口路径
        bodyString.append(JSON.toJSONString("com.dongnaoedu.tony.service.UserService")).append("\r\n");
        // 服务版本号
        bodyString.append(JSON.toJSONString("0.0.0")).append("\r\n");
        // 服务方法名
        bodyString.append(JSON.toJSONString("getUserNameById")).append("\r\n");
        // 参数描述符,JVM参数描述符，按顺序填写
        bodyString.append(JSON.toJSONString("Ljava/lang/String;")).append("\r\n");
        // 参数值,需要进行转换(序列化)
        bodyString.append(JSON.toJSONString("123")).append("\r\n");
        // 隐式参数(为dubbo框架拓展提供的,是一个对象)
        bodyString.append("{}").append("\r\n");
        byte[] body = bodyString.toString().getBytes();
        System.out.println("body内容如下：");
        System.out.println(bodyString);
        System.out.println("1. 请求的body内容组装完成");

        // 头部 ，实际就是 长度为16的 字节数组。接下来的操作难度太大，建议计算机基础不好的朋友，听听思路就好
        byte[] header = new byte[16];
        // 魔数，short类型。 标记为一个报文的开头
        byte[] magicArray = ByteUtil.short2bytes((short) 0xdabb);
        System.arraycopy(magicArray, 0, header, 0, 2);

        // flag(1B)标识位，0000 0101 ，8个01 表示数据序列化方式、请求/响应、事件信息、是否需要响应
        header[2] = (byte) 0xC6;

        // 响应状态(1) , 请求报文默认是 0
        header[3] = 0x00;

        // messageId(8B)，每次请求的唯一ID， 8字节 = Long
        byte[] messageId = ByteUtil.long2bytes(1L);
        System.arraycopy(messageId, 0, header, 4, 8);

        // bodyLength(4B)，后面的内容长度，4字节 = Int
        byte[] bodyLength = ByteUtil.int2bytes(body.length);
        System.arraycopy(bodyLength, 0, header, 12, 4);

        System.out.println("2. header部分组装完成");

        // 拼装请求报文
        byte[] request = new byte[body.length + header.length];
        System.arraycopy(header, 0, request, 0, header.length);
        System.arraycopy(body, 0, request, 16, body.length);
        // -------------------------------组装完成

        // 4、发送请求
        dubboClient.write(ByteBuffer.wrap(request));

        // 5、 接收响应
        ByteBuffer response = ByteBuffer.allocate(1025);
        dubboClient.read(response);
        System.out.println("##########最终调用结果");
        System.out.println(new String(response.array()));
    }
}
