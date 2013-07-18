/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.bootstrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.bootstrap.internal.NettyRatpackService;
import org.ratpackframework.bootstrap.internal.RatpackChannelInitializer;
import org.ratpackframework.bootstrap.internal.ServiceBackedServer;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Builds a {@link RatpackServer}.
 */
@SuppressWarnings("UnusedDeclaration")
public class RatpackServerBuilder {

  /**
   * The port that Ratpack apps use unless otherwise specified ({@value}).
   */
  public static final int DEFAULT_PORT = 5050;

  private final Handler handler;
  private final File baseDir;

  private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

  private int port = DEFAULT_PORT;
  private InetAddress address;

  private String publicUrl = null;

  /**
   * Create a new builder, with the given handler as the "application".
   *
   * @param handler The handler for all requests.
   * @param baseDir The directory that will serve as the initial {@link org.ratpackframework.file.FileSystemBinding} for all handlers
   */
  public RatpackServerBuilder(Handler handler, File baseDir) {
    this.handler = handler;
    this.baseDir = baseDir;
  }

  /**
   * The port that the application should listen to requests on.
   * <p>
   * Defaults to {@value #DEFAULT_PORT}.
   *
   * @return The port that the application should listen to requests on.
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the port that the application should listen to requests on.
   *
   * @param port The port.
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * The address of the interface that the application should bind to.
   * <p>
   * A value of null causes all interfaces to be bound. Defaults to null.
   *
   * @return The address of the interface that the application should bind to.
   */
  @Nullable
  public InetAddress getAddress() {
    return address;
  }

  /**
   * Sets the address of the interface that the application should bind to.
   * <p>
   * A value of null causes all interfaces to be bound.
   *
   * @param address The address.
   */
  public void setAddress(@Nullable InetAddress address) {
    this.address = address;
  }

  /**
   * The number of worker threads for handling application requests.
   * <p>
   * If the value is greater than 0, a thread pool (of this size) will be created for servicing requests. This allows handlers
   * to perform blocking operations.
   * <p>
   * If the value is 0 or less, no thread pool will be used to handle requests. This means that the handler will be called on the
   * same thread that accepted the request. This means that handlers SHOULD NOT block in their operation.
   * <p>
   * The default value for this property is calculated as: {@code Runtime.getRuntime().availableProcessors() * 2}
   *
   * @return The number of worker threads to use to execute the handler.
   */
  public int getWorkerThreads() {
    return workerThreads;
  }

  /**
   * Sets the number of worker threads to use to execute the handler.
   *
   * @param workerThreads The number of worker threads to use to execute the handler.
   * @see #setWorkerThreads(int)
   */
  public void setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
  }

  /**
   * Constructs a new server based on the builder's state.
   * <p>
   * The returned server has not been started.
   *
   * @return A new, not yet started, Ratpack server.
   */
  public RatpackServer build() {
    InetSocketAddress address = buildSocketAddress();
    ChannelInitializer<SocketChannel> channelInitializer = buildChannelInitializer();
    NettyRatpackService service = new NettyRatpackService(address, channelInitializer);
    return new ServiceBackedServer(service);
  }

  private InetSocketAddress buildSocketAddress() {
    return (address == null) ? new InetSocketAddress(port) : new InetSocketAddress(address, port);
  }

  private ChannelInitializer<SocketChannel> buildChannelInitializer() {
    return new RatpackChannelInitializer(workerThreads, handler, baseDir,publicUrl);
  }

  /**
   * The public url used for redirects.
   *
   * @return The public url.
   */
  @Nullable
  public String getPublicUrl() {
    return publicUrl;
  }

  /**
   * Sets the public url used by redirects.
   *
   * @param publicUrl The public url.
   */
  public void setPublicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
  }
}
