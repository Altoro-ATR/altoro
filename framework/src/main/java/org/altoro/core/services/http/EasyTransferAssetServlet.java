package org.altoro.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.altoro.api.GrpcAPI;
import org.altoro.api.GrpcAPI.EasyTransferAssetMessage;
import org.altoro.api.GrpcAPI.EasyTransferResponse;
import org.altoro.api.GrpcAPI.Return.response_code;
import org.altoro.common.crypto.SignInterface;
import org.altoro.common.crypto.SignUtils;
import org.altoro.core.Wallet;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.core.config.args.Args;
import org.altoro.core.exception.ContractValidateException;
import org.altoro.core.services.http.JsonFormat.ParseException;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;


@Component
@Slf4j
public class EasyTransferAssetServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
    EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
    boolean visible = false;
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      visible = Util.getVisiblePost(input);
      EasyTransferAssetMessage.Builder build = EasyTransferAssetMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      byte[] privateKey = wallet.pass2Key(build.getPassPhrase().toByteArray());
      SignInterface ecKey = SignUtils.fromPrivate(privateKey, Args.getInstance()
          .isECKeyCryptoEngine());
      byte[] owner = ecKey.getAddress();
      TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(owner));
      builder.setToAddress(build.getToAddress());
      builder.setAssetName(ByteString.copyFrom(build.getAssetId().getBytes()));
      builder.setAmount(build.getAmount());

      TransactionCapsule transactionCapsule;
      transactionCapsule = wallet
          .createTransactionCapsule(builder.build(), ContractType.TransferAssetContract);
      transactionCapsule.sign(privateKey);
      GrpcAPI.Return result = wallet.broadcastTransaction(transactionCapsule.getInstance());
      responseBuild.setTransaction(transactionCapsule.getInstance());
      responseBuild.setResult(result);
      response.getWriter().println(Util.printEasyTransferResponse(responseBuild.build(), visible));
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
      returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
      returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    } catch (ContractValidateException e) {
      returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    }
  }
}
