/*
Copyright PNF Software, Inc.

    https://www.pnfsoftware.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.pnf.plugin.pdf.address;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class AddressImpl implements IAddress {
    private long[] range;
    private String address;
    private String label;
    private String comment;

    public AddressImpl(long[] range, String address, String label, String comment) {
        this.range = range;
        this.address = address;
        this.label = label;
        this.comment = comment;
    }

    @Override
    public long[] getRange() {
        return range;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public static IAddress nullObject() {
        return new AddressImpl(new long[]{0, 0}, null, null, null);
    }

}
