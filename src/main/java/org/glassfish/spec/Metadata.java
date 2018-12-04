/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.spec;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Represents an API JAR {@code MANIFEST.MF} entries.
 * @author Romain Grecourt
 */
public final class Metadata {

    /**
     * Bundle Symbolic Name.
     */
    private String bundleSymbolicName;

    /**
     * Bundle Spec Version.
     */
    private String bundleSpecVersion;

    /**
     * Bundle Version.
     */
    private String bundleVersion;

    /**
     * Jar Extension Name.
     */
    private String jarExtensionName;

    /**
     * Jar Specification Version.
     */
    private String jarSpecificationVersion;

    /**
     * Jar Implementation Version.
     */
    private String jarImplementationVersion;

    /**
     * Properties.
     */
    private Properties properties = null;

    /**
     * Entry name for Bundle Symbolic Name.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    /**
     * Entry name for Bundle Spec Version.
     */
    public static final String BUNDLE_SPEC_VERSION = "BundleSpecVersion";

    /**
     * Entry name for Bundle Version.
     */
    public static final String BUNDLE_VERSION = "Bundle-Version";

    /**
     * Entry name for Jar Extension Name.
     */
    public static final String JAR_EXTENSION_NAME = "Extension-Name";

    /**
     * Entry name for Jar Specification Version.
     */
    public static final String JAR_SPECIFICATION_VERSION =
            "Specification-Version";

    /**
     * Entry name for Jar Implementation Version.
     */
    public static final String JAR_IMPLEMENTATION_VERSION =
            "Implementation-Version";

    /**
     * List of collected errors.
     */
    private final List<String> errors;

    /**
     * All the metadata entry names.
     */
    private static final String[] KEYS = {
        BUNDLE_SYMBOLIC_NAME,
        BUNDLE_SPEC_VERSION,
        BUNDLE_VERSION,
        JAR_EXTENSION_NAME,
        JAR_SPECIFICATION_VERSION,
        JAR_IMPLEMENTATION_VERSION
    };

    /**
     * Create a new {@link Metadata} instance.
     * @param bsn bundle symbolic name
     * @param bsv bundle spec version
     * @param bv bundle version
     * @param jen jar extension name
     * @param jsv jar spec version
     * @param jiv jar implementation version
     * @param errs errors
     */
    Metadata(final String bsn, final String bsv, final String bv,
            final String jen, final String jsv, final String jiv,
            final List<String> errs) {

        this.bundleSymbolicName =
                bsn != null ? bsn : "";
        this.bundleSpecVersion =
                bsv != null ? bsv : "";
        this.bundleVersion =
                bv != null ? bv : "";
        this.jarExtensionName =
                jen != null ? jen : "";
        this.jarSpecificationVersion =
                jsv != null ? jsv : "";
        this.jarImplementationVersion =
                jiv != null ? jiv : "";

        Objects.requireNonNull(errs, "errors in null");
        this.errors = errs;
        this.properties = new Properties();
        properties.put("spec.bundle.symbolic-name", bundleSymbolicName);
        properties.put("spec.bundle.spec.version", bundleSpecVersion);
        properties.put("spec.bundle.version", bundleVersion);
        properties.put("spec.extension.name", jarExtensionName);
        properties.put("spec.specification.version", jarSpecificationVersion);
        properties.put("spec.implementation.version", jarImplementationVersion);
    }

    /**
     * Create a new {@link Metadata} instance.
     * @param bsn bundle symbolic name
     * @param bsv bundle spec version
     * @param bv bundle version
     * @param jen jar extension name
     * @param jsv jar spec version
     * @param jiv jar implementation version
     */
    Metadata(final String bsn, final String bsv, final String bv,
            final String jen, final String jsv, final String jiv) {

        this(bsn, bsv, bv, jen, jsv, jiv, new LinkedList<String>());
    }

    /**
     * Derive the Bundle Spec Version from OSGi headers.
     * @param headers the headers to process
     * @return the bundle spec version if found, otherwise an empty string.
     */
    @SuppressWarnings("checkstyle:LineLength")
    private static String getBundleSpecVersion(final String headers) {

        // TODO extract exported package version
        // to use with the fromJar approach
        Map<String, List<String>> res = new HashMap<String, List<String>>();

        String[] headersTokens = headers.split(";");
        if (headersTokens.length > 1) {
            ArrayList<String> curHeader = new ArrayList<String>();
            String key = "";
            for (int i = 0; i < headersTokens.length; i++) {
                if (!(headersTokens[i].startsWith("uses:=")
                        || headersTokens[i].startsWith("version="))) {
                    key = headersTokens[i];
                } else {
                    if (headersTokens[i].startsWith("version=")) {
                        String[] lastToken = headersTokens[i].split(",");
                        curHeader.add(lastToken[0]);
                        res.put(key, new ArrayList<String>(curHeader));
                        if (headersTokens[i].length() > lastToken[0].length()) {
                            key = headersTokens[i].substring(lastToken[0].length() + 1);
                            curHeader.clear();
                        }
                    } else if (headersTokens[i].startsWith("uses:=")) {
                        if (i != headersTokens.length - 1
                                && !headersTokens[i + 1].startsWith("version=")) {
                            String[] lastToken = headersTokens[i].split(",");
                            curHeader.add(headersTokens[i].substring(0,
                                    headersTokens[i].length()
                                            - (lastToken[lastToken.length - 1].length())));
                            res.put(key, new ArrayList<String>(curHeader));
                            key = lastToken[lastToken.length - 1];
                            curHeader.clear();
                        } else {
                            curHeader.add(headersTokens[i]);
                        }
                    }
                }
            }
        } else {
            res.put(headers, Collections.EMPTY_LIST);
        }
        return "";
    }

    /**
     * Create a new {@link Metadata} instance from a JAR file.
     * @param jar the JAR file to process
     * @return the created {@link Metadata} instance
     * @throws IOException if an error occurs while reading JAR entries
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static Metadata fromJar(final JarFile jar) throws IOException {
        ZipEntry e = jar.getEntry("META-INF/MANIFEST.MF");
        InputStream is = jar.getInputStream(e);
        Manifest manifest = new Manifest(is);

        List<String> errors = new LinkedList<String>();
        String[] mdata = new String[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(BUNDLE_SPEC_VERSION)) {
                // skip bundleSpecVersion
                continue;
            }
            mdata[i] = manifest.getMainAttributes().getValue(KEYS[i]);
            if (mdata[i] == null) {
                errors.add(new StringBuilder()
                        .append("ERROR: ")
                        .append(KEYS[i])
                        .append(" not found in MANIFEST")
                        .toString());
            }
        }

        // TODO parse exported-packages to resolve bundleSpecVersion
        return new Metadata(mdata[0], mdata[1], mdata[2], mdata[3], mdata[4],
                mdata[5], errors);
    }

    /**
     * Get the bundle symbolic name entry.
     * @return bundle symbolic name
     */
    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    /**
     * Get the bundle spec version entry.
     * @return bundle spec version
     */
    public String getBundleSpecVersion() {
        return bundleSpecVersion;
    }

    /**
     * Get the bundle version entry.
     * @return bundle version
     */
    public String getBundleVersion() {
        return bundleVersion;
    }

    /**
     * Get the jar extension name entry.
     *
     * @return jar extension name
     */
    public String getJarExtensionName() {
        return jarExtensionName;
    }

    /**
     * Get the jar specification version entry.
     * @return jar specification version
     */
    public String getJarSpecificationVersion() {
        return jarSpecificationVersion;
    }

    /**
     * Get the jar implementation version entry.
     *
     * @return jar implementation version
     */
    public String getjarImplementationVersion() {
        return jarImplementationVersion;
    }

    /**
     * Get the metadata properties.
     * @return metadata properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Get the metadata errors.
     * @return the list of errors
     */
    public List<String> getErrors() {
        return errors;
    }
}
