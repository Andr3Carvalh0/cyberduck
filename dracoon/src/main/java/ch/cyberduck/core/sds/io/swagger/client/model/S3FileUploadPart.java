/*
 * DRACOON API
 * REST Web Services for DRACOON<br>built at: 2020-09-09 08:12:59<br><br>This page provides an overview of all available and documented DRACOON APIs, which are grouped by tags.<br>Each tag provides a collection of APIs that are intended for a specific area of the DRACOON.<br><br><a title='Developer Information' href='https://developer.dracoon.com'>Developer Information</a>&emsp;&emsp;<a title='Get SDKs on GitHub' href='https://github.com/dracoon'>Get SDKs on GitHub</a><br><br><a title='Terms of service' href='https://www.dracoon.com/terms/general-terms-and-conditions/'>Terms of service</a>
 *
 * OpenAPI spec version: 4.23.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package ch.cyberduck.core.sds.io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * S3 file upload part information
 */
@Schema(description = "S3 file upload part information")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-09-15T09:21:49.036118+02:00[Europe/Zurich]")
public class S3FileUploadPart {
  @JsonProperty("partNumber")
  private Integer partNumber = null;

  @JsonProperty("partEtag")
  private String partEtag = null;

  public S3FileUploadPart partNumber(Integer partNumber) {
    this.partNumber = partNumber;
    return this;
  }

   /**
   * Corresponding part number
   * @return partNumber
  **/
  @Schema(required = true, description = "Corresponding part number")
  public Integer getPartNumber() {
    return partNumber;
  }

  public void setPartNumber(Integer partNumber) {
    this.partNumber = partNumber;
  }

  public S3FileUploadPart partEtag(String partEtag) {
    this.partEtag = partEtag;
    return this;
  }

   /**
   * Corresponding part ETag
   * @return partEtag
  **/
  @Schema(required = true, description = "Corresponding part ETag")
  public String getPartEtag() {
    return partEtag;
  }

  public void setPartEtag(String partEtag) {
    this.partEtag = partEtag;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    S3FileUploadPart s3FileUploadPart = (S3FileUploadPart) o;
    return Objects.equals(this.partNumber, s3FileUploadPart.partNumber) &&
        Objects.equals(this.partEtag, s3FileUploadPart.partEtag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partNumber, partEtag);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class S3FileUploadPart {\n");
    
    sb.append("    partNumber: ").append(toIndentedString(partNumber)).append("\n");
    sb.append("    partEtag: ").append(toIndentedString(partEtag)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
