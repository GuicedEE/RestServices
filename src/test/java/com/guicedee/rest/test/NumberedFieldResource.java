package com.guicedee.rest.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Reproduces a reported regression where DTO fields whose names end in a digit
 * ({@code image1}, {@code image2}, {@code image3}) allegedly stop rendering on the
 * REST service after the Jackson 3 migration.
 * <p>
 * The DTOs mirror the production shape exactly: private digit-suffixed fields with
 * Lombok-style getters, class-level {@link JsonInclude}{@code (NON_EMPTY)} and
 * {@link JsonAutoDetect}{@code (fieldVisibility = ANY, getterVisibility = NONE)}.
 */
@ApplicationPath("rest")
@Path("numbered")
@Produces(MediaType.APPLICATION_JSON)
public class NumberedFieldResource
{
    /**
     * Nested DTO mirroring {@code MaterialDTO}.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class MaterialDTO
    {
        private String url;

        public MaterialDTO()
        {
        }

        public MaterialDTO(String url)
        {
            this.url = url;
        }

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }
    }

    /**
     * DTO with digit-suffixed fields, mirroring the reported shape.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class GalleryDTO
    {
        private MaterialDTO image1;
        private MaterialDTO image2;
        private MaterialDTO image3;

        public MaterialDTO getImage1() { return image1; }
        public void setImage1(MaterialDTO image1) { this.image1 = image1; }

        public MaterialDTO getImage2() { return image2; }
        public void setImage2(MaterialDTO image2) { this.image2 = image2; }

        public MaterialDTO getImage3() { return image3; }
        public void setImage3(MaterialDTO image3) { this.image3 = image3; }
    }

    /**
     * Returns a fully-populated gallery — all numbered fields should render.
     */
    @GET
    @Path("gallery")
    public GalleryDTO gallery()
    {
        GalleryDTO gallery = new GalleryDTO();
        gallery.setImage1(new MaterialDTO("a.png"));
        gallery.setImage2(new MaterialDTO("b.png"));
        gallery.setImage3(new MaterialDTO("c.png"));
        return gallery;
    }

    /**
     * Returns a gallery with only {@code image1} populated — demonstrates that null
     * numbered fields are omitted by {@code NON_EMPTY}/{@code NON_NULL} inclusion.
     */
    @GET
    @Path("gallery-partial")
    public GalleryDTO galleryPartial()
    {
        GalleryDTO gallery = new GalleryDTO();
        gallery.setImage1(new MaterialDTO("a.png"));
        return gallery;
    }
}

