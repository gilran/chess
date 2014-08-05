package com.gilran.chess.server;

import com.gilran.chess.JsonParser;
import com.gilran.chess.Proto.ErrorResponse;
import com.gilran.chess.Proto.Status;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

/**
 * A request handler for GET requests.
 *
 * <p>The request handler uses the public methods of ServiceImpl in order to
 * identify the available web-service methods. In order to add a method, there
 * is no need to change the request handler. All that is needed is to add a
 * public method with the signature Status(<T extends Message>, final Callback)
 * to ServiceImpl, and it will be exported by the request handler.
 * <p>The request handler hides the use of JSON from the ServiceImpl, that uses
 * protobuf messages. This is done in order to decouple the network protocol
 * from the Chess service protocol messages. By replacing the request handler
 * with another class that translates protobuf messages from and to some other
 * format we can replace the network protocol without making any changes in
 * implementation of other modules.
 */
@Path("/")
public class RequestHandler {
  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(
      Thread.currentThread().getStackTrace()[0].getClassName());

  /** The Chess service implementation class. */
  private static ServiceImpl service;
  /** A from available method names to the methods themselves. */
  private static Map<String, Method> methodsMap;

  static {
    service = new ServiceImpl();
    methodsMap = Maps.newHashMap();

    Set<Method> methods = Sets.newHashSet(service.getClass().getMethods());
    methods.removeAll(
        Lists.newArrayList(service.getClass().getSuperclass().getMethods()));

    for (Method method : methods) {
      Class<?>[] paramTypes = method.getParameterTypes();

      Preconditions.checkState(paramTypes.length == 2);
      Preconditions.checkState(Message.class.isAssignableFrom(paramTypes[0]));
      Preconditions.checkState(paramTypes[1] == ServiceImpl.Callback.class);
      Preconditions.checkState(method.getReturnType().equals(Status.class));

      LOGGER.info("Registering method: " + method.getName());
      methodsMap.put(method.getName(), method);
    }
  }

  /**
   * A generic handler for all get requests.
   *
   * <p>This method identifies the ServiceImpl method that should be used for
   * the request, translates the request JSON to protobuf message, and runs the
   * ServiceImpl method.
   * <p>The invokation is async, allowing the method to perform other async
   * operations and delay the response as needed, without holding the thread.
   */
  @GET
  @Path("/{method}")
  public void get(
      @PathParam("method") String methodName,
      @QueryParam("r") String requestJson,
      @Suspended final AsyncResponse asyncResponse) {
    LOGGER.info(methodName + "(" + requestJson + ")");
    if (requestJson == null || requestJson.isEmpty()) {
      asyncResponse.resume(
          Response.status(Response.Status.BAD_REQUEST).build());
      return;
    }

    Method method = methodsMap.get(methodName);
    if (method == null) {
      asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).build());
      return;
    }

    Message request =
      JsonParser.toProto(requestJson, method.getParameterTypes()[0]);
    if (request == null || !request.isInitialized()) {
      asyncResponse.resume(
          Response.status(Response.Status.BAD_REQUEST).build());
      return;
    }

    ServiceImpl.Callback callback = new ServiceImpl.Callback() {
      public void run(Message response) {
        Preconditions.checkNotNull(response);
        asyncResponse.resume(JsonParser.toJson(response));
      }
    };

    Status status;
    try {
      status = (Status) method.invoke(service, request, callback);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (status != Status.OK) {
      callback.run(ErrorResponse.newBuilder().setStatus(status).build());
    }
  }
}
