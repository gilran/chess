package com.gilran.chess;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

/** A parser that translates JSON to protobuf and protobuf to JSON. */
public class JsonParser {
  /** Translate the given JSON string to a protobuf of the given type. */
  public static <T> Message toProto(String json, Class<T> type) {
    if (json == null) {
      return null;
    }

    Message.Builder builder = null;

    try {
      builder = (Message.Builder) type.getMethod("newBuilder").invoke(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      JsonFormat.merge(json, builder);
    } catch (ParseException e) {
      return null;
    }

    if (!builder.isInitialized()) {
      return null;
    }
    return builder.build();
  }

  /** Translate the given protobuf message to a JSON string. */
  public static String toJson(Message message) {
    return JsonFormat.printToString(message);
  }
}
