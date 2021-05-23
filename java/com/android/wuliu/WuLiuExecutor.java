package com.android.wuliu;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WuLiuExecutor {
  private static final ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();

  public static void execute(Runnable runnable) {
    threadPoolExecutor.submit(runnable);
  }
}
