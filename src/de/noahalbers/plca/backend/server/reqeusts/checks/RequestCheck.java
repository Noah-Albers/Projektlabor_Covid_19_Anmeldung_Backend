package de.noahalbers.plca.backend.server.reqeusts.checks;

import java.io.IOException;

import de.noahalbers.plca.backend.server.reqeusts.Request;

@FunctionalInterface
public interface RequestCheck{
	public boolean checkRequest(Request request) throws IOException;
}