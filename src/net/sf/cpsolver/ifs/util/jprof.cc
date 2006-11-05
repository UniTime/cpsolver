#include <jvmpi.h>

/*
 * IFS 1.0 (Iterative Forward Search)
 * Copyright (C) 2006 Tomas Muller
 * muller@ktiml.mff.cuni.cz
 * Lazenska 391, 76314 Zlin, Czech Republic
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

// global jvmpi interface pointer
static JVMPI_Interface *jvmpi_interface;

// profiler agent entry point
extern "C" { 
  JNIEXPORT jint JNICALL JVM_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    // get jvmpi interface pointer
    if ((jvm->GetEnv((void **)&jvmpi_interface, JVMPI_VERSION_1)) < 0) {
      return JNI_ERR;
    } 
    return JNI_OK;
  }

  JNIEXPORT jlong JNICALL Java_net_sf_ifs_cpsolver_util_JProf_getCurrentThreadCpuTime
  (JNIEnv *, jclass) {
    // return 0 if agent not initialized
    return jvmpi_interface == 0 ? 0 : jvmpi_interface->GetCurrentThreadCpuTime();
  }
}

