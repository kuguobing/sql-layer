/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */
package com.akiban.direct.script;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.akiban.direct.DirectContext;
import com.akiban.direct.DirectModule;

/**
 * <p>
 * Adapter to load, compile and run Javascript code as {@link DirectModule}.
 * This implementation takes a path specified as a parameter to read JS source
 * code. A WatchService is set up to watch for changes in this file; if the file
 * changes the new version is automatically compiled and installed at the next
 * call to {@link #eval(Map)}.
 * </p>
 * <p>
 * Example: <code><pre>
 * curl -X PUT "localhost:9000/api/direct/module/test
 *           ?language=js&name=Sample&url=file:/Users/peter/workspace/sandbox/bin/
 *           &file=sample.js&idempotent=true"
 *           
 * curl -X GET localhost:9000/api/direct/Sample/test
 * </pre></code> The first curl command loads a Javascript module named
 * sample.js and installs it with the endpoint name "Sample". It is declared to
 * be idempotent which means its {@link #eval(Map)} method is called in response
 * to a GET request, as exemplified by the second curl command.
 * </p>
 * 
 * @author peter
 */
public class JSModule implements DirectModule {

    private boolean isGetEnabled;
    private DirectContext context;
    ScriptEngine engine;
    private CompiledScript compiled;
    private Bindings bindings;
    private Source source;

    interface Source {

        Reader getReader() throws Exception;

        boolean isChanged() throws Exception;

        void setParams(Map<String, List<String>> params);

        void start() throws Exception;

        void stop() throws Exception;
    }

    /**
     * Receive a DirectContext instance. This provides a source for JDBC
     * Connections, among other things.
     * 
     * @param context
     */
    @Override
    public void setContext(DirectContext context) {
        this.context = context;
    }

    public DirectContext getContext() {
        return context;
    }

    /**
     * Called when the module is loaded, after
     * {@link #setContext(DirectContext)} and {@link #setParams(Map)}.
     * 
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        source.start();
        loadModule();
    }

    /**
     * Called when this module is removed.
     * 
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        source.stop();
    }

    /**
     * Declares whether this module changes any state information on the server.
     * If <code>true<code>, this module is accessible only by GET requests. If
     * <code>false</code> it is accessible only by PUT and POST requests.
     * 
     * @return <code>true</code> if this module should be accessed through GET
     *         requests
     */
    @Override
    public boolean isGetEnabled() {
        return isGetEnabled;
    }

    /**
     * Evaluate the loaded Javascript code. This implementation passes two
     * variables to the Javascript module, named <code>dcontext</code> (a
     * DirectContext) and <code>params</code> the parameters passed with the the
     * REST request (See {@link javax.ws.rs.core.MultivaluedMap}.
     * 
     * @param params
     *            The parameters passed in the REST request
     */
    @Override
    public Object eval(Map<String, List<String>> params) throws Exception {
        if (source.isChanged()) {
            loadModule();
        }
        bindings.put("params", params);
        return compiled.eval(bindings);
    }

    /**
     * Get a JDBC Connection. The value returned is actually a
     * {@link com.akiban.sql.embedded.JDBCConnection}.
     * 
     * @return a Connection
     */
    public java.sql.Connection getConnection() {
        return context.getConnection();
    }

    /**
     * Create a JDBC Statement instance. This is a convenience method equivalent
     * to {@link com.akiban.sql.embedded.JDBCConnection#createStatement()} on
     * the connection provided by the DirectContext.
     * 
     * @return a Statement on which queries can be executed.
     * @throws SQLException
     */
    public java.sql.Statement createStatement() throws SQLException {
        return context.getConnection().createStatement();
    }

    public void setParams(Map<String, List<String>> params) {
        String isi = getFirst(params, "get");
        isGetEnabled = isi == null ? false : isi.equalsIgnoreCase("true");
        if (getFirst(params, "source") != null) {
            source = new JSModuleSourceString();
        } else if (getFirst(params, "file") != null) {
            source = new JSModuleSourceFile();
        } else {
            throw new IllegalArgumentException("Source parameter not specified");
        }
        source.setParams(params);
    }

    static String getFirst(Map<String, List<String>> params, String name) {
        List<String> values = params.get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    private synchronized void loadModule() throws Exception {
        if (engine == null) {
            ScriptEngineManager manager = new ScriptEngineManager(getClass().getClassLoader());
            engine = manager.getEngineByName("js");
        }
        Compilable compiler = (Compilable) engine;
        compiled = compiler.compile(source.getReader());
        bindings = engine.createBindings();
        bindings.put("dcontext", context);

    }

}
