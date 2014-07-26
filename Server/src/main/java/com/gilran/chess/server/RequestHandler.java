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

@Path("/")
public class RequestHandler {
  private static final Logger LOGGER = Logger.getLogger(
      Thread.currentThread().getStackTrace()[0].getClassName());

  private static ServiceImpl service;
  private static Map<String, Method> methodsMap;

  static {
    service = new ServiceImpl();
    methodsMap = Maps.newHashMap();

    Set<Method> methods = Sets.newHashSet(service.getClass().getMethods());
    methods.removeAll(
        Lists.newArrayList(service.getClass().getSuperclass().getMethods()));

    for (Method method : methods) {
      Class<?>[] paramTypes = method.getParameterTypes();

      LOGGER.info("paramTypes.length = " + paramTypes.length);
      Preconditions.checkState(paramTypes.length == 2);
      Preconditions.checkState(Message.class.isAssignableFrom(paramTypes[0]));
      Preconditions.checkState(paramTypes[1] == ServiceImpl.Callback.class);
      Preconditions.checkState(method.getReturnType().equals(Status.class));

      methodsMap.put(method.getName(), method);
    }
  }

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
