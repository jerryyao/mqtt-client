/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.stayfool.client.message;

/**
 * Basic abstract message for all MQTT protocol messages.
 *
 * @author andrea
 */
public abstract class AbstractMessage {
    //type
    protected boolean m_dupFlag;
    protected QOSType m_qos;
    protected boolean m_retainFlag;
    protected int m_remainingLength;
    protected byte m_messageType;

    public byte getMessageType() {
        return m_messageType;
    }

    public void setMessageType(byte messageType) {
        this.m_messageType = messageType;
    }

    public boolean isDupFlag() {
        return m_dupFlag;
    }

    public void setDupFlag(boolean dupFlag) {
        this.m_dupFlag = dupFlag;
    }

    public QOSType getQos() {
        return m_qos;
    }

    public void setQos(QOSType qos) {
        this.m_qos = qos;
    }

    public boolean isRetainFlag() {
        return m_retainFlag;
    }

    public void setRetainFlag(boolean retainFlag) {
        this.m_retainFlag = retainFlag;
    }

    /**
     * TOBE used only internally
     */
    public int getRemainingLength() {
        return m_remainingLength;
    }

    /**
     * TOBE used only internally
     */
    public void setRemainingLength(int remainingLength) {
        this.m_remainingLength = remainingLength;
    }
}
