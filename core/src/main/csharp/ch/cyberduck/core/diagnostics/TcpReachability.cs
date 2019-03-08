﻿// 
// Copyright (c) 2010-2019 Yves Langisch. All rights reserved.
// http://cyberduck.io/
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
// 
// Bug fixes, suggestions and comments should be sent to:
// feedback@cyberduck.io
// 

using System;
using System.Diagnostics;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using ch.cyberduck.core;
using ch.cyberduck.core.diagnostics;

namespace Ch.Cyberduck.Core.Diagnostics
{
    public class TcpReachability : Reachability
    {
        public bool isReachable(Host h)
        {
            try
            {
                if (h.getProtocol().getScheme().name().Equals("http") || h.getProtocol().getScheme().name().Equals("https"))
                {
                    WebRequest.DefaultWebProxy.Credentials = CredentialCache.DefaultNetworkCredentials;
                    WebRequest request =
                        WebRequest.Create(new HostUrlProvider().withUsername(false).withPath(true).get(h));
                    request.GetResponse();
                }
                else
                {
                    TcpClient c = new TcpClient(h.getHostname(), h.getPort());
                    c.Close();
                }

                return true;
            }
            catch (Exception)
            {
                return false;
            }
        }

        public void diagnose(Host h)
        {
            Process.Start("Rundll32.exe", "ndfapi,NdfRunDllDiagnoseIncident");
        }

        Reachability.Monitor Reachability.monitor(Host h, Reachability.Callback callback)
        {
            return new NetworkChangeMonitor(h, callback);
        }
    }

    class NetworkChangeMonitor : Reachability.Monitor
    {
        private readonly Reachability.Callback _callback;

        public NetworkChangeMonitor(Host h, Reachability.Callback callback)
        {
            _callback = callback;
        }

        public Reachability.Monitor start()
        {
            NetworkChange.NetworkAvailabilityChanged += Changed;
            return this;
        }

        public Reachability.Monitor stop()
        {
            NetworkChange.NetworkAvailabilityChanged -= Changed;
            return this;
        }

        void Changed(object sender, NetworkAvailabilityEventArgs args)
        {
            _callback.change();
        }
    }
}
