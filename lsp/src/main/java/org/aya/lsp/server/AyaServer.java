// Copyright (c) 2020-2022 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.lsp.server;

import org.aya.cli.library.incremental.CompilerAdvisor;
import org.aya.lsp.actions.ComputeTerm;
import org.aya.lsp.models.ComputeTermResult;
import org.aya.lsp.models.HighlightResult;
import org.aya.lsp.utils.Log;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AyaServer implements LanguageClientAware, LanguageServer {
  private final @NotNull AyaService service;

  public AyaServer() {
    this(CompilerAdvisor.inMemory());
  }

  public AyaServer(@NotNull CompilerAdvisor advisor) {
    this.service = new AyaService(advisor);
  }

  @JsonRequest("aya/load")
  @SuppressWarnings("unused")
  public @NotNull CompletableFuture<@NotNull List<HighlightResult>> load(Object uri) {
    return CompletableFuture.supplyAsync(() -> service.reload().asJava());
  }

  @JsonRequest("aya/computeType")
  @SuppressWarnings("unused")
  public @NotNull CompletableFuture<@NotNull ComputeTermResult> computeType(ComputeTermResult.Params input) {
    return CompletableFuture.supplyAsync(() -> service.computeTerm(input, ComputeTerm.Kind.type()));
  }

  @JsonRequest("aya/computeNF")
  @SuppressWarnings("unused")
  public @NotNull CompletableFuture<@NotNull ComputeTermResult> computeNF(ComputeTermResult.Params input) {
    return CompletableFuture.supplyAsync(() -> service.computeTerm(input, ComputeTerm.Kind.nf()));
  }

  @Override public void connect(@NotNull LanguageClient client) {
    var c = (AyaLanguageClient) client;
    Log.init(c);
    service.connect(c);
  }

  @Override public @NotNull CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    return CompletableFuture.supplyAsync(() -> {
      var cap = new ServerCapabilities();
      cap.setTextDocumentSync(TextDocumentSyncKind.None);
      var workCap = new WorkspaceServerCapabilities();
      var workOps = new WorkspaceFoldersOptions();
      workOps.setSupported(true);
      workOps.setChangeNotifications(true);
      workCap.setWorkspaceFolders(workOps);
      cap.setCompletionProvider(new CompletionOptions(true, Collections.singletonList(
        "QWERTYUIOPASDFGHJKLZXCVBNM.qwertyuiopasdfghjklzxcvbnm+-*/_[]:")));
      cap.setWorkspace(workCap);
      cap.setDefinitionProvider(true);
      cap.setReferencesProvider(true);
      cap.setHoverProvider(true);
      cap.setRenameProvider(new RenameOptions(true));
      cap.setDocumentHighlightProvider(true);
      cap.setCodeLensProvider(new CodeLensOptions(true));
      cap.setInlayHintProvider(true);
      cap.setDocumentSymbolProvider(true);
      cap.setWorkspaceSymbolProvider(true);
      cap.setFoldingRangeProvider(true);

      var folders = params.getWorkspaceFolders();
      // In case we open a single file, this value will be null, so be careful.
      // Make sure the library to be initialized when loading files.
      if (folders != null) folders.forEach(f ->
        service.registerLibrary(Path.of(URI.create(f.getUri()))));

      return new InitializeResult(cap);
    });
  }

  @Override public @NotNull CompletableFuture<Object> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  @Override public void exit() {
    Log.i("Goodbye");
  }

  @Override public @NotNull AyaService getTextDocumentService() {
    return service;
  }

  @Override public @NotNull AyaService getWorkspaceService() {
    return service;
  }
}
