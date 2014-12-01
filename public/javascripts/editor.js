var options = {
  basePath: '',
  theme: {
    base: '/assets/stylesheets/epiceditor/base/epiceditor.css',
    preview: '/assets/stylesheets/base.css',
    editor: '/assets/stylesheets/epiceditor/editor/epic-dark.css'
  },
  autogrow: {
    minHeight: 400
  }
};

var editor = new EpicEditor(options).load();

$('#submitPost').click(function(event) {
  event.preventDefault();

  var data = {
    title: $('#title').val(),
    slug: $('#postSlug').data('post-slug'),
    markdown: editor.exportFile(null, 'text'),
    html: editor.exportFile(null, 'html'),
    published: $('#published').is(':checked')
  };

  // delete the slug field if it's an empty string or undefined
  if (!data.slug) {
    delete data.slug
  }

  $.ajax({
    type: 'POST',
    contentType: 'application/json; charset=utf-8',
    url: '/save',
    dataType: 'json',
    data: JSON.stringify(data),
    success: function(data) {
      window.location.href = "/blog/" + data.slug;
    }
  });

});
