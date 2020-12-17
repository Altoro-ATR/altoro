package org.altoro.test;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.crypto.SignInterface;
import org.altoro.common.crypto.SignUtils;
import org.altoro.common.utils.ByteArray;
import org.altoro.common.utils.StringUtil;
import org.altoro.common.utils.Utils;
import org.altoro.core.config.args.Args;
import org.apache.commons.codec.binary.Hex;

/**
 * @Auther: little liu
 * @Date: 2020/11/28/9:14
 * @Description:
 */
@Slf4j
public class Test {
    public static void main(String[] args) {
        SignInterface sign = SignUtils.getGeneratedRandomSign(Utils.getRandom(),
                Args.getInstance().isECKeyCryptoEngine());
        byte[] priKey = sign.getPrivateKey();
        byte[] address = sign.getAddress();
        String priKeyStr = Hex.encodeHexString(priKey);
        String base58check = StringUtil.encode58Check(address);
        String hexString = ByteArray.toHexString(address);
        JSONObject jsonAddress = new JSONObject();
        jsonAddress.put("address", base58check);
        jsonAddress.put("hexAddress", hexString);
        jsonAddress.put("privateKey", priKeyStr);
        logger.info(jsonAddress.toJSONString());
    }
}
